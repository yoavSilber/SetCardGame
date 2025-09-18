package bguspl.set.ex;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import bguspl.set.Env;
import java.util.Random;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {
   // protected int[] actions;
    protected BlockingQueue<Integer> keys;
    protected Dealer dealer;
    protected int tokenCounter=0;
    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    private Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;

    private Boolean frozen;

    private long freezetime;

    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;
        this.dealer = dealer;
        this.keys= new LinkedBlockingQueue<Integer>(3);
        frozen = false;
        freezetime = 0;
    }
    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        if (!human) 
            createArtificialIntelligence();
        while (!terminate) {
            if(dealer.gameOn){

                try {
                    if(freezetime>0){
                        treat_forzen_state();
                    }
                    int s = keys.take();//take out of the queue
                    if(table.tokensPlaced[s].contains(id)){
                        table.removeToken(id,s);
                        tokenCounter--;
                    }
                    else if(tokenCounter<3&&dealer.gameOn&&!table.EmptySlots.contains(s)){
                        table.placeToken(id,s);
                        tokenCounter++;
                        if(tokenCounter==3){//add to the list of players that need to be checked
                            dealer.add_player_with_token(this);
                            synchronized(this){
                                wait();
                            }
                        }
                    }
                } catch (InterruptedException e) {}             
            }
        }      
        if (!human) try { aiThread.join(); } catch (InterruptedException ignored) {}
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    }

    private void treat_forzen_state()//added function fot taking care of the frozen state
    {
        synchronized(this){
            while(frozen)
            {
                env.ui.setFreeze(id, freezetime);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {}
                freezetime -= 1000;
                if(freezetime == 0){
                    frozen = false;
                    env.ui.setFreeze(id, 0);
                }
            }
        }
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very, very smart AI (!)       
        aiThread = new Thread(() -> {
            env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
            while (!terminate) {   
                try {
                    if(dealer.gameOn){
                        Random rand = new Random();
                        int press = (int)rand.nextInt(env.config.tableSize) ;
                        if(!table.EmptySlots.contains(press))
                            keys.put(press);
                    }
                } catch (InterruptedException e) {
                }
            }
            env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }
    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        if (!terminate) {
            terminate = true;
            dealer.gameOn = false;
            if (!human){
                try {
                    aiThread.interrupt();
                    aiThread.join();
                } catch (InterruptedException ignored) {}
            }      
            try {
                playerThread.interrupt();
                playerThread.join();
            } catch (InterruptedException e) {}
        }   
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
            try {
                if(!table.EmptySlots.contains(slot))
                    keys.add(slot);
            } catch (IllegalStateException e) {}
    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        env.ui.setScore(id, ++score);
        keys.clear();
        tokenCounter=0;
        synchronized(this){
            frozen = true;
            freezetime = env.config.pointFreezeMillis;
        }
    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {//we did
        synchronized(this){
            frozen = true;
            freezetime = env.config.penaltyFreezeMillis;
        }  
    }

    public int score() {
        return score;
    }
    //*******************assist functions******************************************************************* */
   
    public int[] getPlayerTokens(){
        int[] tokens = new int[3];
        int place=0;//place in the array
        for(int i=0;i<table.tokensPlaced.length&& place<3;i++){
            for(int token:table.tokensPlaced[i]){
                if (token==id&&table.slotToCard[i]!=null){
                    tokens[place]=table.slotToCard[i];
                    place++;
                }
            }
        }
        return tokens;
    }
    public Thread getPlayerThread(){
        return playerThread;
    }
}
