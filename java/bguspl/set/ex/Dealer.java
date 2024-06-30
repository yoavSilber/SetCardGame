package bguspl.set.ex;

import bguspl.set.Env;
import java.util.ArrayList;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
//import java.util.concurrent.BlockingQueue;
//import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.Collections;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {
    //added fields:
     protected Queue <Player> players_needed_checking;
    //  private ArrayList<Integer> checkOverLap=new ArrayList<Integer>();
    /**
     * The game environment object.
     */
    private final Env env;

    public volatile boolean gameOn = false;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;

    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());   
        this.players_needed_checking = new LinkedBlockingQueue <Player>();//added     
    }
    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        //initialize all player threads
        for (Player p : players) {
            new Thread(p).start();
        }
        while (!shouldFinish()) {
            placeCardsOnTable();
            timerLoop();
            updateTimerDisplay(true);    
        }
        announceWinners();
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    }
    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() { 
        gameOn = true;      
        reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis;
        updateTimerDisplay(false);
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            sleepUntilWokenOrTimeout();
            updateTimerDisplay(false);
            synchronized(players_needed_checking){
                while(!players_needed_checking.isEmpty()){
                    Player p = players_needed_checking.poll();
                    checkSet(p);
               }   

            }
                   
            placeCardsOnTable();
        }
        gameOn = false;
    }
    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        if(!terminate) {
            terminate = true;
            for(Player p : players) {//terminate all players
                p.terminate();
            }
        }
    }
    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */
    private void removeCardsFromTable(int [] cards) {//we changed the signature
        //only when someone found a set ,cards should be on size 3
        //activated only when a set is approved by testSet
       
            synchronized(table){
                for(int i=0;i<cards.length;i++){
                    int card=cards[i];
                    if (table.cardToSlot[card]!=null){
                        table.removeCard(table.cardToSlot[card]);    
                    }  
                }
            }
            restartTimerDisplayAfterSet();
        
    }
    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {    
        Collections.shuffle(deck);
        Collections.shuffle(table.EmptySlots);
            synchronized(table.tableLock){
                while(table.EmptySlots.size()>0 && deck.size()>0){
                    int card;
                    int slot=table.EmptySlots.get(0); 
                    card=deck.get(0);
                    deck.remove(0);  
                    table.placeCard(card,slot);
                }  
            }  
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private void sleepUntilWokenOrTimeout() {//we did
        try{
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {//we did
        if(reset) { 
            removeAllCardsFromTable();
            env.ui.setCountdown(env.config.turnTimeoutMillis, false);
            players_needed_checking.clear();
        }   
        else{
            long remainingTime = reshuffleTime - System.currentTimeMillis();
            //if the remaining time is less than the warning time, display the warning
            if ( remainingTime<=env.config.turnTimeoutWarningMillis) { 
                 if(remainingTime<=0){
                    env.ui.setCountdown(0, true);
                 }
                 else{
                    env.ui.setCountdown(remainingTime, true);
                 }
            }
            else {
                env.ui.setCountdown(remainingTime, false);
            }
        
        }
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        
            synchronized(table.tableLock){
                for(int i=0;i<env.config.tableSize;i++){//i is the slot in the grid
                    if(table.slotToCard[i]!=null){
                        deck.add(table.slotToCard[i]);
                        table.removeCard(i);
                    }
                }
            }
        
            synchronized(table.tableLock){
                removeAllTokens();
                removeAllCards();
            } 
         
          
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        LinkedList<Integer> scores = new LinkedList<Integer>();         
        int maxScore = 0;
        //finds max score
        for(Player p : players){
            if(p.score() > maxScore){
                 maxScore = p.score();
            }
        }
        //finds all players with max score
        for(Player p : players){
            if(p.score() == maxScore){
                scores.add(p.id);
            }
        } 
        //transform the list into an array
        int[] winners= new int[scores.size()];
        for(int i=0;i<scores.size();i++){
            winners[i] = scores.get(i);
        }
        //announce the winner with winners array
        env.ui.announceWinner(winners);  
    }
    //*************************added functions **********************************************************************************/
    public void removeAllTokens(){
        
            synchronized(table.tableLock){
                for(int i=0;i<table.tokensPlaced.length;i++){//i is the slot in the grid
                    //ArrayList<Integer> playerTokens=table.tokensPlaced [i];
                    if (table.tokensPlaced[i].size()!=0){ 
                        int size=table.tokensPlaced[i].size();
                        for(int j=0;j<size;j++){
                            table.removeToken(table.tokensPlaced [i].get(0),i);
                        }  
                    }
                }
            }
        
           
        for(int i=0;i<players.length;i++){
            players[i].tokenCounter=0;
        }

    }
    public  void removeAllCards(){
                  
            synchronized(table.tableLock){
                for(int i=0;i<env.config.tableSize;i++){//i is the slot in the grid
                    if(table.slotToCard[i]!=null){
                        table.removeCard(i);
                    }
                }
            
        }
       
    }
    public void checkSet(Player p){
        synchronized(table){
            int[] cards = p.getPlayerTokens();
            //check if the size is valid for a set
            if(p.tokenCounter==3){//the case someone  took the set before the player
                //check if the set is valid-and give point or penalty according to the result
                if (env.util.testSet(cards)){  
                    removeTokens(p,cards);
                    removeCardsFromTable(cards);     
                    p.point();
                    placeCardsOnTable();
                }
                else{          
                    p.penalty();
                } 
                synchronized(p){
                    p.notify();
                }
            }
            synchronized(p){
                p.notify();
            }  
        }             
    } 
    public void removeTokens (Player p,int[] cards){
        
            synchronized(table.tableLock){
                for(int i=0;i<cards.length;i++){
                    if(cards[i]!=0){
                        table.removeToken(p.id,table.cardToSlot[cards[i]]);
                        p.tokenCounter--;
                        int size=table.tokensPlaced[table.cardToSlot[cards[i]]].size();
                        for(int j=0;j<size;j++){//check if there are more tokens to remove
                            Player toRemoveToken=getPlayer(table.tokensPlaced[table.cardToSlot[cards[i]]].get(0));
                            table.removeToken(table.tokensPlaced[table.cardToSlot[cards[i]]].get(0),table.cardToSlot[cards[i]]);
                            toRemoveToken.tokenCounter--;
                            
                       }
                        
                    }
                }
            }
        
    }   
    private void restartTimerDisplayAfterSet() {//we did
        reshuffleTime=System.currentTimeMillis()+env.config.turnTimeoutMillis;
        long remainingTime = reshuffleTime - System.currentTimeMillis();
        env.ui.setCountdown(remainingTime, false);
        
    }  
    void add_player_with_token(Player e)
    {
        synchronized(players_needed_checking){
            players_needed_checking.add(e);
        }
    }  
    protected Player getPlayer(int id){
        for(Player p:players){
            if(p.id==id){
                return p;
            }
        }
        return null;
    }
}
