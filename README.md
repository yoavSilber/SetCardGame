# SetCardGame

4 THE GAME DESIGN
4.1 The Cards & Features
Cards are represented in the game by int values from 0 to 80.
Each card has 4 features with size 3. Therefore, the features of a card can be represented as an
array of integers of length 4. Each cell in the array represents a feature. The value in the cell
represents one of the 3 possible values of the feature. 
4.2 The Table
The table is the data structure for the game. It is a passive object that is used by the dealer and
the players to share data (more on the dealer and players later).
The table holds the placed cards on a grid of 3x4, and keeps track of which token was placed by
whom.
4.3 The Players
For each player, a separate thread is created to represent the player’s entity.
The player object manages the player data (such as the player id and type - human/non-human).
For non-human players, another thread is created to simulate key presses.
You must maintain a queue of incoming actions (which are dispatched by key presses – either
from the keyboard or from the simulator). The queue size must be equal to the number of cards
that form a legal set (3).
The player thread consumes the actions from the queue, placing or removing a token in the
corresponding slot in the grid on the table.
Once the player places his third token on the table, he must notify the dealer and wait until the
dealer checks if it is a legal set or not. The dealer then gives him either a point or a penalty
accordingly.
The penalty for marking an illegal set is getting frozen for a few seconds (i.e., not being able to
perform any actions). However, even when marking a legal set, the player gets frozen for a
second.
4.4 The Dealer
The dealer is represented by a single thread, which is the main thread in charge of the game
flow. It handles the following tasks:
▪ Creates and runs the player threads.
▪ Dealing the cards to the table.
▪ Shuffling the cards.
▪ Collecting the cards back from the table when needed.
▪ Checking if the tokens that were placed by the player form a legal set.
▪ Keeping track of the countdown timer.
▪ Awarding the player with points and/or penalizing them.
▪ Checking if any legal sets can be formed from the remaining cards in the deck.
▪ Announcing the winner(s).
Notes:
1. When dealing cards to the table and collecting cards from the table the dealer thread should
sleep for a short period (as is already written for you in Table::placeCard and
Table::removeCard methods provided in the skeleton files).
2. Checking user sets should be done “fairly” – if 2 players try to claim a set at roughly the
same time, they should be serviced by the dealer in “first come first served” (FIFO) order.
Any kind of synchronization mechanism used for this specific part of the program must take
this into consideration.
4.5 The Graphic User Interface & Keyboard Input
Note: do not make any changes to any of the components in this section.
4.5.2 The Input Manager
The handling of the input from the keyboard has also been written for you in the InputManager
class. It will automatically call Player::keyPressed method and pass as an argument the slot
number of the card that corresponds to the key that was pressed.
The slot number is: 𝒄𝒐𝒍𝒖𝒎𝒏 + 𝒕𝒐𝒕𝒂𝒍 𝒄𝒐𝒍𝒖𝒎𝒏𝒔 ∗ 𝒓𝒐𝒘
4.5.3 The Window Manager
When the user clicks the close window button, the class WindowManager that we provided you
with, automatically calls Dealer::terminate method of the dealer thread, and Player::terminate
method for each opened player thread.
4.6 Other Components Supplied for You
Note: do not make any changes to any of the components in this section.
4.6.1 Class Main
Loads all the required components, initializes the logger, and creates and runs the dealer thread.
4.6.2 Class Env
Helper class for sharing the Logger, Config, UserInterface and Util classes.
You can use the env.logger for self-checks. However, you are NOT ALLOWED to log any events in
“Level.SEVERE” (this level warnings will be used to check your code in the automatic tests).
4.6.3 Class Config
Loads and parses the configuration file2
. The configuration object is passed to any class that may
need it. You should use the configuration fields where applicable. A bonus of +2 points will be
awarded for fully supporting all configuration fields, and avoiding use of any magic numbers.
The Config class tries to load config.properties from the current working directory. If it cannot
find it, it will look for it in the resources directory and if it can’t find it there too, it will use the
predefined properties in the class.
4.6.4 Class Util
This class contains several utility methods that you may use freely in your implementation
(converting card id to array of features and finding/testing if a collection of cards contains/is a
legal set).
2 Not all the fields of the Config class are identical to what appears in config.properties (e.g., some fields in
the file are in seconds and in the class are in milliseconds, and there are some additional fields in the class
for convenience).
