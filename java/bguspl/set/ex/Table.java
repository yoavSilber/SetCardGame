package bguspl.set.ex;

import bguspl.set.Env;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This class contains the data that is visible to the player.
 *
 * @inv slotToCard[x] == y iff cardToSlot[y] == x
 */
public class Table {
    //added fields:
    protected ArrayList<Integer> [] tokensPlaced;
    protected ArrayList<Integer> EmptySlots;
    protected Object tableLock = new Object();
 
   
   
    /**
     * The game environment object.
     */
    private final Env env;


    /**
     * Mapping between a slot and the card placed in it (null if none).
     */
    protected final Integer[] slotToCard; // card per slot (if any)

    /**
     * Mapping between a card and the slot it is in (null if none).
     */
    protected final Integer[] cardToSlot; // slot per card (if any)

    /**
     * Constructor for testing.
     *
     * @param env        - the game environment objects.
     * @param slotToCard - mapping between a slot and the card placed in it (null if none).
     * @param cardToSlot - mapping between a card and the slot it is in (null if none).
     */
    public Table(Env env, Integer[] slotToCard, Integer[] cardToSlot) {

        this.env = env;
        this.slotToCard = slotToCard;
        this.cardToSlot = cardToSlot;
        this.tokensPlaced = new ArrayList[env.config.tableSize];//added
        for (int i = 0; i < env.config.tableSize; i++){
            tokensPlaced[i] = new ArrayList<Integer>();
        }
        this.EmptySlots = new ArrayList<Integer> ();//added
        //fill EmptySlots with empty slots
        for (int i = 0; i < env.config.tableSize; i++){
            EmptySlots.add(i);
        }
    }

    /**
     * Constructor for actual usage.
     *
     * @param env - the game environment objects.
     */
    public Table(Env env) {

        this(env, new Integer[env.config.tableSize], new Integer[env.config.deckSize]);
    }

    /**
     * This method prints all possible legal sets of cards that are currently on the table.
     */
    public void hints() {
        List<Integer> deck = Arrays.stream(slotToCard).filter(Objects::nonNull).collect(Collectors.toList());
        env.util.findSets(deck, Integer.MAX_VALUE).forEach(set -> {
            StringBuilder sb = new StringBuilder().append("Hint: Set found: ");
            List<Integer> slots = Arrays.stream(set).mapToObj(card -> cardToSlot[card]).sorted().collect(Collectors.toList());
            int[][] features = env.util.cardsToFeatures(set);
            System.out.println(sb.append("slots: ").append(slots).append(" features: ").append(Arrays.deepToString(features)));
        });
    }

    /**
     * Count the number of cards currently on the table.
     *
     * @return - the number of cards on the table.
     */
    public synchronized int countCards() {
        int cards = 0;
        for (Integer card : slotToCard)
            if (card != null)
                ++cards;
        return cards;
    }

    /**
     * Places a card on the table in a grid slot.
     * @param card - the card id to place in the slot.
     * @param slot - the slot in which the card should be placed.
     *
     * @post - the card placed is on the table, in the assigned slot.
     */
    public synchronized void placeCard(int card, int slot) {//we did
        cardToSlot[card] = slot;
        slotToCard[slot] = card;
        EmptySlots.remove((Object)slot);//******* */
        env.ui.placeCard(card,slot);//changed
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}
    }

    /**
     * Removes a card from a grid slot on the table.
     * @param slot - the slot from which to remove the card.
     */
    public synchronized void removeCard(int slot) {//we did
        int cardRemoved = slotToCard[slot];
        cardToSlot[cardRemoved] = null;
        slotToCard[slot] = null;
        EmptySlots.add(slot);
        env.ui.removeCard(slot);
        //remove token from slot
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}
    }

    /**
     * Places a player token on a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot on which to place the token.
     */
    public synchronized void placeToken(int player, int slot) {//we did
        if(!EmptySlots.contains(slot)){
            tokensPlaced[slot].add(player);
            env.ui.placeToken(player, slot);
        }
    }

    /**
     * Removes a token of a player from a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot from which to remove the token.
     * @return       - true iff a token was successfully removed.
     */
    public synchronized boolean removeToken(int player, int slot) {//we did
        if (!tokensPlaced[slot].contains(player))
            return false;
        
        tokensPlaced[slot].remove((Object)player);
        env.ui.removeToken(player, slot);
        return true;
    }
}
