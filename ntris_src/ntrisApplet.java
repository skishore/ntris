package ntris_src;

import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.SortedSet;
import java.util.TreeSet;
import static ntris_src.Constants.*;
import static ntris_src.GUI.*;
import static ntris_src.KeyRepeater.RepeatHandler;
import static ntris_src.ntrisColor.*;

public class ntrisApplet extends Applet implements ActionListener, FocusListener, MouseListener, RepeatHandler, Runnable {
    private static final long serialVersionUID = 2350;
    private static final boolean verbose = false;

    private GUI gui;

    // display constants
    private int SQUAREWIDTH;
    private int BORDER;
    private int SIDEBOARD;
    private int SCREENWIDTH;
    private int SCREENHEIGHT;

    // frameCount is a rhythm variable, incremented mod MAXFRAMECOUNT each frame
    private int frameCount = 0;
    private static final int MAXFRAMECOUNT = 6720;
    private static final int FRAMERATE = 60;
    private static final int TICKS_PER_SEC = 1000;
    private static final int FRAMEDELAY = (TICKS_PER_SEC / FRAMERATE);
    // redraw everything every FULLUPDATE frames
    private static final int FULLUPDATE = 1024*FRAMERATE;

    // the score at which we transition between difficulty levels
    private static final int SCOREINTERVAL = 60;
    private static final double MINR = 0.1;
    private static final double MAXR = 0.9;
    private static final int HALFRSCORE = 480;
    private int difficultyLevels;
    private int[] numBlockTypes = new int[10];
    private int numSymbols;
    Block[] blockData = new Block[13000];

    // boards which conduct all Tetris logic and handle drawing
    private Board board;
    private Board singleplayerBoard;
    private Board myBoard;
    private Board advBoard;
    private int highScore = -1;
    private boolean scoreSent = false;

    // graphics and pseudorandom number generation
    private long nextSeed;
    private PRG prg;
    private Image backBuffer;
    private Graphics bbGraphics;
    private KeyRepeater keyRepeater;

    // client constants and data structures
    private String IPAddress;
    private static final int DEFAULTPORT = 1618;
    private static final int TIMEOUT = 8;
    private static final int COMMANDSPERFRAME = 4;
    private Client client;
    private Queue<String> ntrisEvents;
    private int timeWaited;

    // client state and modes
    private static final int INGUI = 0;
    private static final int WAITING = 1;
    private static final int INGAME = 2;
    private int clientMode = INGUI;
    private int gameMode = SINGLEPLAYER;
    private boolean online = false;
    private String name = null;

    // data about other online users and the user the client invited
    private SortedSet<UserData> names = new TreeSet<UserData>();
    private boolean seeking = false;
    private String invite = null;
    private String adv = null;

    @Override
    public void init() {
        initWindow();
        try {
            openBlockData();
        } catch (IOException e) {
            System.out.println("Failed to open block data");
        }
        initBoards();
        gui = new GUI(this, this);
        gui.createGamePanel(backBuffer, this);

        IPAddress = getParameter("ip");
        if (IPAddress == null)
            IPAddress = "localhost";
        client = new Client(IPAddress, DEFAULTPORT, 2*FRAMEDELAY);
        ntrisEvents = new LinkedList<String>();
        resetClient();

        gui.addFocusListener(this);
        this.addMouseListener(this);
        this.requestFocus();
    }   

    public void focusGained(FocusEvent e) { } 

    public void focusLost(FocusEvent e) { 
        if ((clientMode == INGAME) && (board.getBoardState() == PLAYING)) {
            board.repeaterPress(PAUSE);
            board.repeaterRelease(PAUSE);
            keyRepeater.releaseAll();
        }
    }

    public void mouseClicked(MouseEvent e) { } 

    public void mousePressed(MouseEvent e) { 
        this.requestFocus();
        if (clientMode == INGAME)
            gui.requestFocus();
    }   

    public void mouseReleased(MouseEvent e) { } 

    public void mouseEntered(MouseEvent e) { } 

    public void mouseExited(MouseEvent e) { }  

    private void initWindow() {
        if (getParameter("size") != null)
            SQUAREWIDTH = Integer.parseInt(getParameter("size"));
        if (SQUAREWIDTH <= 0)
            SQUAREWIDTH = 21;
        BORDER = SQUAREWIDTH;
        SIDEBOARD = 7 * SQUAREWIDTH / 2;

        SCREENWIDTH = 2*(SQUAREWIDTH*NUMCOLS + SIDEBOARD);
        SCREENHEIGHT = SQUAREWIDTH*(NUMROWS - MAXBLOCKSIZE + 1);

        setBackground(WHITE);
        setSize(new Dimension(SCREENWIDTH + 2*BORDER, SCREENHEIGHT + 2*BORDER));

        backBuffer = createImage(SCREENWIDTH + 2*BORDER, SCREENHEIGHT + 2*BORDER);
        bbGraphics = backBuffer.getGraphics();

        keyRepeater = new KeyRepeater(this, 120, 30);
    }

    private void openBlockData() throws IOException {
        String line;
        int x, y, level, zeroIndex;
        BufferedReader myfile;
        URL url;
        URLConnection connection;

        url = this.getClass().getResource("blockData.dat");

        connection = url.openConnection();
        myfile = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        // the file begins with the number of different types of blocks at each difficulty level
        line = myfile.readLine();
        difficultyLevels = Integer.parseInt(line);
        for (int i = 0; i < difficultyLevels; i++) {
            line = myfile.readLine();
            numBlockTypes[i] = Integer.parseInt(line);
        }
        line = myfile.readLine();
        numSymbols = Integer.parseInt(line);

        level = 0;
        zeroIndex = Block.charToSymbol('0') + numBlockTypes[difficultyLevels - 1];

        // blank line before the block data
        line = myfile.readLine();

        for (int i = 0; i < numBlockTypes[difficultyLevels - 1] + numSymbols; i++) {
            blockData[i] = new Block();

            if (i >= numBlockTypes[difficultyLevels - 1])
                line = myfile.readLine();

            // the first two lines of this block's data record the starting position
            line = myfile.readLine();
            blockData[i].x = NUMCOLS/2 + Integer.parseInt(line);
            line = myfile.readLine();
            blockData[i].y = Integer.parseInt(line);
            // next, the number of squares in the block
            line = myfile.readLine();
            blockData[i].numSquares = Integer.parseInt(line);
            // read each square's local coordinates from memory
            for (int j = 0; j < blockData[i].numSquares; j++) {
                line = myfile.readLine();
                x = Integer.parseInt(line);
                line = myfile.readLine();
                y = Integer.parseInt(line);
                blockData[i].squares[j] = new Point(x, y);
            }
            // lastly, read the color
            if (i < numBlockTypes[difficultyLevels - 1]) {
                if (i > numBlockTypes[level])
                    level++;
                line = myfile.readLine();
                blockData[i].color[0] = colorCode(Integer.parseInt(line));
                blockData[i].color[1] = difficultyColor(blockData[i].color[0], level, difficultyLevels-1);
            } else {
                if ((zeroIndex <= i) && (i < zeroIndex + 10))
                    blockData[i].color[1] = difficultyColor(STAIRCASE, i-zeroIndex+1, difficultyLevels-1);
            }

            // record the block's height in blockData
            blockData[i].height = calculateBlockHeight(blockData[i]);

            // blank line after each block's data
            line = myfile.readLine();
        }

        // get data about any blocks that do not rotate
        line = myfile.readLine();
        x = Integer.parseInt(line);
        // there are x blocks that do not rotate - read their indices now
        for (int i = 0; i < x; i++) {
            line = myfile.readLine();
            blockData[Integer.parseInt(line)].rotates = false;
        }
        myfile.close();
    }

    private int calculateBlockHeight(Block block) {
        int highest = 0;
        int lowest = 0;

        for (int i = 0; i < block.numSquares; i++) {
            if (block.squares[i].y < lowest)
                lowest = block.squares[i].y;
            if (block.squares[i].y > highest)
                highest = block.squares[i].y;
        }
        return highest - lowest + 1;
    }

    private void initBoards() {
        int numBlocks = numBlockTypes[difficultyLevels - 1];

        prg = new PRG(0);

        singleplayerBoard = new Board(BORDER + SCREENWIDTH/4, BORDER, SQUAREWIDTH, SIDEBOARD,
                numBlocks, blockData, SINGLEPLAYER);
        singleplayerBoard.loadSprites(this, SCREENWIDTH/2, SCREENHEIGHT);

        myBoard = new Board(BORDER, BORDER, SQUAREWIDTH, SIDEBOARD, numBlocks, blockData, MULTIPLAYER);
        myBoard.loadSprites(this, SCREENWIDTH/2, SCREENHEIGHT);
        advBoard = new Board(BORDER + 9*SCREENWIDTH/16, BORDER + SCREENHEIGHT/8,
                3*SQUAREWIDTH/4, 3*SIDEBOARD/4, numBlocks, blockData, MULTIPLAYER);
    }

    private void resetClient() {
        resetClient(false);
    }

    private void resetClient(boolean lostConnection) {
        if (clientMode == WAITING)
            clientMode = INGUI;

        clearInvites();

        gui.setMode(OFFLINEGUI);
        if (lostConnection) {
            gui.setStatus("disconnected from server");
        } else {
            gui.setStatus("offline");
            highScore = -1;
        }
        
        if ((clientMode == INGAME) && (gameMode == MULTIPLAYER))
            endGame();
    
        client.disconnect();
        name = null;
        timeWaited = 0; 
        online = false;

        if (lostConnection && (clientMode == INGUI))
            gui.showDialog("Lost connection to server.");
    }

    private void clearInvites() {
        seeking = false;
        invite = null;
        adv = null;
        gui.changeMatchButtons(false, false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (clientMode == WAITING)
            return;

        String action = e.getActionCommand();

        if (action.equals("play")) {
            gameMode = SINGLEPLAYER;
            startGame();
        } else if (action.equals("invite")) {
            String opp = gui.getSelectedName();
            if ((opp != null) && !opp.equals(name)) {
                blockOnServer();
                client.sendCommand("ntris." + opp);
            } else if (!seeking) {
                blockOnServer();
                client.sendCommand("ntris");
            }
        } else if (action.equals("auto")) {
            if (!seeking) {
                blockOnServer();
                client.sendCommand("ntris");
            }
        } else if (action.equals("cancel")) {
            blockOnServer();
            client.sendCommand("cancel");
        } else if (action.equals("help")) {
            gui.showDialog(HELPFILE, false);   
        } else if (action.equals("scores")) {
            blockOnServer();
            client.sendCommand("record");
        } else if (action.equals("logon") || action.equals("signup")) {
            String logonInfo = Client.sanitizeLogonString(gui.getLogonInfo(), 2);
            if (logonInfo != null) {
                if (client.connect()) {
                    online = true;
                    blockOnServer();
                    client.sendCommand(action + "." + logonInfo);
                } else {
                    resetClient(true);
                }
            } else {
                gui.showDialog("<html>Invalid username or password.<ul>" +
                               "<li>Name and password must be alphanumeric</li>" +
                               "<li>Neither string can be empty</li></ul></html>");
            }
        } else if (action.equals("logoff")) {
            client.sendCommand("logoff");
            resetClient();
        } else if (action.equals("send")) {
            client.sendCommand("talk." + Client.escape(gui.getMessage()));
        }
    }

    private void blockOnServer() {
        if (online) {
            clientMode = WAITING;
            gui.setMode(DISABLEDGUI);
            gui.setStatus("waiting on server...");
        }
    }

    private void unblock() {
        clientMode = INGUI;
        gui.setMode(ONLINEGUI);
        gui.setStatus("logged in as " + name);
    }

    @Override
    public void repeaterPress(int key) {
        if (clientMode == INGAME) {
            board.repeaterPress(key);
            if (key == ESCAPE) {
                if (gameMode == MULTIPLAYER) {
                    String result = (board.getBoardState() < COUNTDOWN ? "ingame" : "not");
                    client.sendCommand("quit." + result);
                }
                endGame();
            }
        }
    }

    @Override
    public void repeaterRelease(int key) {
        if (clientMode == INGAME)
            board.repeaterRelease(key);
    }

    private void startGame() {
        if (gameMode == SINGLEPLAYER) {
            board = singleplayerBoard;
            prg.seedPRG(System.currentTimeMillis(), true);
        } else {
            nextSeed = prg.generate();
            board = myBoard;
            advBoard.resetBoard();
        }

        keyRepeater.reset();
        board.resetBoard();
        clientMode = INGAME;

        gui.showGameCard();
        gui.addKeyListener(keyRepeater);
        redraw(true);
    }

    private void endGame() {
        if (gameMode == SINGLEPLAYER) {
            int score = board.getScore();
            if (online) {
                if (board.getBoardState() < GAMEOVER)
                    client.sendCommand("result.singleplayer." + score); 
            } else if (score > highScore) {
                highScore = score;
            }
        } else {
            adv = null;
        }
        clientMode = INGUI;

        gui.showButtonCard();
        gui.removeKeyListener(keyRepeater);
        gui.repaint();
    }

    private void checkForCommands() {
        int numCommands = 0;
        String command = client.getCommand();

        while ((command != null) && (numCommands < COMMANDSPERFRAME)) {
            doCommand(command);
            command = client.getCommand();
            numCommands++;
        }
    }

    private void doCommand(String command) {
        if (!online)
            return;
        String[] tokens = command.split("\\.");
        String type = tokens[0];

        if (tokens[0].equals("ntrisEvent")) {
            ntrisEvents.add(tokens[1]);
        } else if (tokens[0].equals("ntris")) {
            setUserState(tokens[1], UserData.INGAME, false);
            setUserState(tokens[2], UserData.INGAME);

            if (tokens[1].equals(name)) {
                if (clientMode == INGAME)
                    endGame();
                unblock();

                clearInvites();
                prg.seedPRG(Long.parseLong(tokens[3]), false);
                adv = tokens[2];
                gameMode = MULTIPLAYER;
                startGame();
            }
        } else if (type.equals("getname")) {
            name = tokens[1];
            unblock(); 

            gui.clearMessages();
            gui.addMessage(">>> Logged on to the ntris server at " +
                           Client.getDateTime());

            names.clear();
            for (int i = 2; i < tokens.length; i++)
                names.add(new UserData(tokens[i], true));
            gui.displayNames(names);

            if (highScore >= 0) {
                client.sendCommand("result.singleplayer." + highScore);
                highScore = -1;
            }
        } else if (type.equals("loggedin")) {
            invalidLogin("User logged in elsewhere.");
        } else if (type.equals("invalidnamepass")) {
            invalidLogin("Invalid name or password.");
        } else if (type.equals("taken")) {
            invalidLogin("Username already taken");
        } else if (type.equals("logon")) {
            names.add(new UserData(tokens[1]));
            gui.displayNames(names);
        } else if (type.equals("logoff")) {
            if (tokens[1].equals(adv))
                endGame();
            if (tokens.length > 2)
                setUserState(tokens[2], UserData.ONLINE, false);

            names.remove(new UserData(tokens[1]));
            gui.displayNames(names);
        } else if (type.equals("seekingGame")) {
            if (tokens[1].equals(name)) {
                unblock();
                seeking = true;
                gui.changeMatchButtons(true, true);
            }
            gui.addMessage(">>> " + tokens[1] + " is seeking a game");
            setUserState(tokens[1], UserData.SEEKING);
        } else if (type.equals("invite")) {
            if (tokens[1].equals(name)) {
                unblock();
                invite = tokens[2];
                gui.changeMatchButtons(true, false);
                gui.addMessage(">>> Invited " + invite + " to a multiplayer game");
            } else if (tokens[2].equals(name)) {
                gui.addMessage(">>> Got a multiplayer invite from " + tokens[1]);
                setUserState(tokens[1], UserData.SEEKING);
            }
        } else if (type.equals("cancel")) {
            if (tokens[1].equals(name)) {
                unblock();
                seeking = false;
                invite = null;
                gui.changeMatchButtons(false, false);
            }
            setUserState(tokens[1], UserData.ONLINE);
        } else if (type.equals("talk")) {
            if (tokens.length > 2) {
                gui.addMessage(tokens[1] + ": " + Client.unescape(tokens[2]));
            } else {
                gui.addMessage(tokens[1] + ": ");
            }
        } else if (tokens[0].equals("quit")) {
            setUserState(tokens[1], UserData.ONLINE, false);
            setUserState(tokens[2], UserData.ONLINE);

            if (tokens[2].equals(name))
                endGame();
        } else if (tokens[0].equals("record")) {
            unblock();

            String record = Client.unescape(tokens[1]);
            for (int i = 2; i < tokens.length; i++)
                record += "\n" + Client.unescape(tokens[i]);
            gui.showDialog(record, false);
        } else if (tokens[0].equals("booted")) {
            resetClient(true);
        }
    }

    private void setUserState(String name, int state) {
        setUserState(name, state, true);
    }

    private void setUserState(String name, int state, boolean display) {
        UserData data = new UserData(name, state);
        if (names.contains(data)) {
            names.remove(data);
            names.add(data);
            gui.displayNames(names);
        }
    }

    private void invalidLogin(String message) {
        clientMode = INGUI;
        gui.setMode(OFFLINEGUI);
        gui.setStatus("unable to log in; retry");
        gui.showDialog(message);
    }

    @Override
    public void start() {
        Thread th = new Thread(this);
        th.start();
    }

    @Override
    public void run() {
        long curTime, lastTime, lastSecond, delay;
        int numFrames = 0;

        curTime = System.currentTimeMillis();
        lastTime = curTime;
        lastSecond = curTime;

        while (true) {
            curTime = System.currentTimeMillis();
            if (curTime > lastTime + (long)FRAMEDELAY) {
                lastTime = curTime;

                if (curTime > lastSecond + (long)TICKS_PER_SEC) {
                    if (verbose)
                        System.out.println("FPS = " + 1.0f * numFrames
                                * TICKS_PER_SEC / (curTime - lastSecond));
                    lastSecond = curTime;
                    numFrames = 1;

                    if (online)
                        sendCheckin();
                } else {
                    numFrames++;
                }

                frameCount = (frameCount + 1) % MAXFRAMECOUNT;

                if (online)
                    checkForCommands();

                if (clientMode == INGAME) {
                    keyRepeater.query(curTime);
                    board.timeStep(frameCount);
                    playTetrisGod(board);
                    if (gameMode == MULTIPLAYER)
                        updateAdvBoard();
                    checkScore();
                    redraw();
                }

                delay = FRAMEDELAY + curTime - System.currentTimeMillis() - 1;
                if (delay > 0) {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ex) {
                    }
                }
                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            }
        }
    }

    private void sendCheckin() {
        if (name != null)
            client.sendCommand("checkin");

        if (!client.isOnline())
            resetClient(true);

        if (clientMode == WAITING) {
            timeWaited++;
            if (timeWaited > TIMEOUT)
                resetClient(true);
        } else {
            timeWaited = 0;
        }
    }

    private void playTetrisGod(Board board) {
        int type, rand, level;
        int numNeeded = board.numBlocksNeeded();
        int score = board.getScore();

        for (int i = 0; i < numNeeded; i++) {
            rand = Math.abs((int)prg.generate());
     
            if (gameMode == SINGLEPLAYER) {
                level = difficultyLevel(score);
     
                if (level > 0) { 
                    type = (rand % (numBlockTypes[level+1] - numBlockTypes[level])) + numBlockTypes[level];
                } else {
                    type = rand % numBlockTypes[0];
                }    
            } else {
                int attack = Math.min(board.getNextAttack(), difficultyLevels - 1);
     
                if (attack == 0) { 
                    type = rand % numBlockTypes[0];
                } else {
                    type = (rand % (numBlockTypes[attack]-numBlockTypes[attack-1])) + numBlockTypes[attack-1];
                }    
            }    
            board.queueBlock(type);
        }
    }

    private int difficultyLevel(int s) {
        double x, p, r;

        if (difficultyLevels == 1)
            return 0;
        // get a random p uniformly from [0, 1]
        p = 1.0 * (prg.generate() % ((int) Math.pow(2, 12))) / Math.pow(2, 12);
        if (p < 0)
            p++;
        // calculate the current ratio r between the probability of different difficulties
        x = 2.0 * (s - HALFRSCORE) / HALFRSCORE;
        r = (MAXR - MINR) * (x / Math.sqrt(1 + x * x) + 1) / 2 + MINR;
        // run through difficulty levels
        for (int i = 1; i < difficultyLevels - 1; i++) {
            x = 2.0 * (s - (SCOREINTERVAL * i)) / SCOREINTERVAL;
            // compare p to a sigmoid which increases to 1 when score passes SCOREINTERVAL*i
            if (p > Math.pow(r, i) * (x / Math.sqrt(1 + x * x) + 1) / 2)
                // if p is still above this sigmoid, we are not yet at this difficulty level
                return i - 1;
        }
        return difficultyLevels - 2;
    }

    private void updateAdvBoard() {
        List<Integer> events = board.getEvents();
        List<Integer> crossEvents;

        if (events != null)
            client.sendCommand("ntrisEvent." + Client.listToString(events));

        events = Client.stringToList(ntrisEvents.poll());
        while (events != null) {
            crossEvents = advBoard.doEvents(events);
            if (crossEvents != null)
                board.doCrossEvents(crossEvents);
            events = Client.stringToList(ntrisEvents.poll());
        }

        if (board.getBoardState() == COUNTDOWN) {
            prg.seedPRG(nextSeed, false);
            nextSeed = prg.generate();
        }
    }

    public void checkScore() {
        if (clientMode != INGAME)
            return;

        if (gameMode == SINGLEPLAYER) {
            if ((board.getBoardState() >= GAMEOVER) && (!scoreSent)) {
                if (name != null) {
                    client.sendCommand("result.singleplayer." + board.getScore());
                } else if (board.getScore() > highScore) {
                    highScore = board.getScore();
                }
                scoreSent = true;
            } else if ((board.getBoardState() < GAMEOVER) && scoreSent) {
                scoreSent = false;
            }
        } else if (gameMode == MULTIPLAYER) {
            if ((board.getBoardState() == COUNTDOWN) && (!scoreSent)) {
                client.sendCommand("result.multiplayer." + board.getScore());
                scoreSent = true;
            } else if ((board.getBoardState() <= GAMEOVER) && scoreSent) {
                scoreSent = false;
            }
        }
    }

    public void redraw() {
        redraw(false);
    }

    public void redraw(boolean fullUpdate) {
        if (frameCount % FULLUPDATE == FULLUPDATE/2 + FRAMERATE/4)
            System.gc();

        fullUpdate = fullUpdate || (frameCount % FULLUPDATE == FRAMERATE/4);
        int xOffset = (gameMode == SINGLEPLAYER ? SCREENWIDTH/4 : 0);
        int width = SCREENWIDTH - 2*xOffset;

        if (fullUpdate)  {
            // Clear the surface
            bbGraphics.setColor(WHITE);
            bbGraphics.fillRect(0, 0, 2*BORDER + SCREENWIDTH, 2*BORDER + SCREENHEIGHT);
            // draw the black background
            bbGraphics.setColor(BLACK);
            bbGraphics.fillRect(xOffset, 0, 2*BORDER + width, 2*BORDER + SCREENHEIGHT);
            // draw the green border
            bbGraphics.setColor(GREEN);
            bbGraphics.drawRect(xOffset + BORDER/2 - 1, BORDER/2 - 1, width + BORDER + 2,
                    SCREENHEIGHT + BORDER + 2);
            bbGraphics.drawRect(xOffset + BORDER/2 - 2, BORDER/2 - 2, width + BORDER + 4,
                    SCREENHEIGHT + BORDER + 4);
        }

        board.draw(bbGraphics, fullUpdate);
        if (gameMode == MULTIPLAYER)
            advBoard.draw(bbGraphics, fullUpdate);
        gui.repaint();
    }
}
