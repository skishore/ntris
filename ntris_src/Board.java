package ntris_src;

import static ntris_src.Constants.*;
import static ntris_src.ntrisColor.*;
import static ntris_src.KeyRepeater.RepeatHandler;

import java.applet.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;
import java.awt.Color;

public class Board implements RepeatHandler {
    // position and size of the board, plus a pointer to the block Data
    private int xPos, yPos;
    private int squareWidth;
    private int sideBoard, boardWidth, boardHeight;
    private int numBlocks;
    private Block[] blockData;

    // in a multiplayer game, record the events and attacks on this board
    private List<Integer> events;
    private List<Integer> attacks;
    private int ATTACKSIZE;

    // board state, score, combo, and current key-input  
    private int score, combo;
    private int boardState, gameMode;
    private List<Integer> moveDir;
    private boolean[] rotated = new boolean[2];
    private boolean dropped;
    private boolean held;
    private boolean entered;

    // up-to-date block data
    private Block curBlock;
    private int curBlockType;
    private int heldBlockType;
    
    // store the preview list, as well as its current animation frame
    private List<Integer> preview;
    private int previewAnim;
    private int previewOffset;

    // flags which tell us how much of the board we need to redraw this frame
    private boolean holdUsed;
    private boolean boardChanged;
    private boolean justHeld;
    // the old block's position - where to erase
    private Block oldBlock;
    
    // description of the blocks already on the board
    private Color[][] board = new Color[NUMCOLS][NUMROWS];
    private int[] blocksInRow = new int[NUMROWS];
    int highestRow;
    
    // pointers to the text sprites, and a flag for drawing them
    private Sprite numbers;
    private Sprite paused;
    private Sprite gameover;    
    private boolean drawSprites;
    
    
    public Board(int x, int y, int square, int side, int n, Block[] data, int mode) {
        xPos = x; yPos = y;
        squareWidth = square;
        ATTACKSIZE = squareWidth/5;
        sideBoard = side;
        boardWidth = squareWidth*NUMCOLS + sideBoard;
        boardHeight = squareWidth*(NUMROWS-MAXBLOCKSIZE+1);
        numBlocks = n;
        blockData = data;
        drawSprites = false;
        
        gameMode = mode;
        if (gameMode == MULTIPLAYER) {
            attacks = new LinkedList<Integer>();
            events = new ArrayList<Integer>();
            events.clear();
        }

        moveDir = new ArrayList<Integer>();
        preview = new LinkedList<Integer>();
        
        resetBoard();
    }
    

    public void loadSprites(Applet applet, int width, int height) {
        MediaTracker tracker = new MediaTracker(applet);
        
        if (gameMode == SINGLEPLAYER) {
            // in the one-player game, we use the numbers, gameover, and paused sprites   
            numbers = new Sprite(applet, 8, 10, "numbers.png", 10, 2, tracker, 0);
            gameover = new Sprite(applet, 192, 32, "gameover.png", 1, 1, tracker, 1);
            // position gameover and paused in the center of the board
            gameover.x = (width - 192)/2 + xPos;
            gameover.y = (height - 32)/2 + yPos;
            paused = new Sprite(applet, 94, 18, "paused.png", 1, 1, tracker, 2);
            paused.x = (width - 94)/2 + xPos;
            paused.y = (height - 18)/2 + yPos;
        } else {
            // load and position the won/lost countdown message
            gameover = new Sprite(applet, 192, 32, "wonlost.png", 2, 3, tracker, 0);
            gameover.x = (width - 192)/2 + xPos;
            gameover.y = (height - 32)/2 + yPos;
        }
        
        try {
            tracker.waitForAll();
        } catch (InterruptedException e) {
            System.err.println("Error loading sprites.");
        }
            
        drawSprites = true;
    }

    public void resetBoard() {
        resetBoard(true);
    }
    
    public void resetBoard(boolean firstReset) {
        if (gameMode == MULTIPLAYER)
            events.add(RESETBOARD);

        // no commands have been given yet, the hold is unused
        rotated[0] = false;
        rotated[1] = false;
        dropped = false;
        held = false;
        entered = !firstReset;
        holdUsed = false;
        justHeld = false;

        // to start off, the board is entirely BLACK
        for (int y = 0; y < NUMROWS; y++) {
            for (int x = 0; x < NUMCOLS; x++) {
                board[x][y] = BLACK;
            }
            blocksInRow[y] = 0;
        }
        highestRow = NUMROWS;
        // the board has changed - redraw
        boardChanged = true;
        // start the preview without animation
        preview.clear();
        previewAnim = 0;
        previewOffset = 0;
        // no block is held - type is -1, the null type
        heldBlockType = -1;
        curBlockType = -1; 
        oldBlock = new Block();
        curBlock = null;

        // reset the score and start the game
        score = 0; 
        combo = 0;
        if (gameMode == MULTIPLAYER)
            attacks.clear();
        boardState = RESET;
    }
    
    public void repeaterPress(int key) {
        if ((key == ENTER) || (key == PAUSE)) {
            if (entered == false) {
                // either pause or enter was pressed - multiple effects
                if (boardState >= GAMEOVER) {
                    if ((key == ENTER) && (gameMode == SINGLEPLAYER))
                        resetBoard();
                } else if (boardState >= PAUSED) {
                    boardState = PLAYING;
                    boardChanged = true;
                    previewAnim = 1;
                } else if (boardState == PLAYING) {
                    if (gameMode == SINGLEPLAYER)
                        boardState = PAUSED;
                }
                entered = true;
            }
        } else {
            // a movement key was pressed - add it to the list of directions
            if (!moveDir.contains(key)) moveDir.add(key);
        }
    }
    
    public void repeaterRelease(int key) {
        if ((key == ENTER) || (key == PAUSE)) {
            entered = false;
        } else {
            // a movement key was released, so remove it from moveDir
            moveDir.remove(new Integer(key));
            // set the key's flag to false so we can rotate / drop / hold again        
            if (key == MOVEUP) 
                rotated[0] = false;
            else if (key == MOVEBACK)
                rotated[1] = false;
            else if (key == MOVEDROP) 
                dropped = false;
            else if (key == MOVEHOLD) 
                held = false;
        }
    }
    
    public int getScore() {
        return score;
    }

    public int getBoardState() {
        return boardState;
    }

    public List<Integer> getEvents() {
        if (events.size() > 0) {
            List<Integer> v = new ArrayList<Integer>(events);
            events.clear();
            return v;
        } else return null;
    }

    public int getNextAttack() {
        if (attacks.size() > 0)
            return attacks.remove(0);
        return 0;
    }
    
    public int numBlocksNeeded() { 
        if (curBlockType == -1)
            return PREVIEW - preview.size() + 1;
        return PREVIEW - preview.size();
    }

    public void queueBlock(int blockType) {
        if (gameMode == MULTIPLAYER) {
            events.add(QUEUEBLOCK);
            events.add(blockType);
        }

        preview.add(blockType);
        if (boardState == RESET) boardState = PLAYING;
    }

    public List<Integer> doEvents(List<Integer> newEvents) {
        List<Integer> crossEvents = new ArrayList<Integer>();
        int i = 0; 
        events = newEvents;
        int size = events.size();
        if (size > MAXEVENTS) size = MAXEVENTS;

        while (i < size) {
            int eventType = events.get(i);
     
            if (eventType == PLACEBLOCK) {
                curBlock.x = events.get(i+1);
                curBlock.y = events.get(i+2);
                curBlock.angle = events.get(i+3);
     
                placeBlock(curBlock);
                curBlock = null;
                i = i + 4; 
            } else if (eventType == GETNEXTBLOCK) {
                getNextBlock((events.get(i+1) == 1)); 
                if (boardState == GAMEOVER) {
                    crossEvents.add(VICTORY);
                }    
                previewAnim = 1; 
                i = i + 2; 
            } else if (eventType == QUEUEBLOCK) {
                queueBlock(events.get(i+1));
                if (attacks.size() > 0)
                    attacks.remove(0);
                i = i + 2; 
            } else if (eventType == SENDATTACK) {
                crossEvents.add(RECEIVEATTACK);
                crossEvents.add(events.get(i+1));
                i = i + 2; 
            } else if (eventType == RECEIVEATTACK) {
                attacks.add(events.get(i+1));
                i = i + 2; 
            } else if (eventType == RESETBOARD) {
                resetBoard();
                i = i + 1; 
            }    
        }    

        events.clear();
        // return the cross events if this board attacked the other
        if (crossEvents.size() == 0) crossEvents = null;
        return crossEvents;
    }

    public void doCrossEvents(List<Integer> crossEvents) {
        int i = 0;
        int size = crossEvents.size();
        if (size > MAXEVENTS) size = MAXEVENTS;

        while (i < size) {
            int eventType = crossEvents.get(i);

            if (eventType == RECEIVEATTACK) {
                attacks.add(crossEvents.get(i+1));
                // log the attack so the other side can see it too
                events.add(RECEIVEATTACK);
                events.add(crossEvents.get(i+1));
                i = i + 2;
            } else if (eventType == VICTORY) {
                boardState = COUNTDOWN;
                score = 1;
                gameover.frameCol = 2;
                i = i + 1;
            }
        }
    }
    
    /** Tetris game logic starts here!                              *
     *  The only public method is timestep(int frame), which        *
     *  runs one step of Tetris logic. This method calls            * 
     *  checkBlock(), shoveaway(), and a host of others            **/

    public void timeStep(int frame) {
        Iterator<Integer> i;
        Point trans = new Point(0, 0);
        int deltaAngle = 0;
        boolean moved = false;

        if (boardState != PLAYING)
            return;

        // don't move a non-existent block
        if (curBlock == null) {
            getNextBlock();
            return;
        }

        int command;
        for (i = moveDir.iterator(); i.hasNext();) {
            command = i.next();
            // record the current movement commands
            if ((command == MOVERIGHT) || (command == MOVELEFT)) {
                // the last command is the only one that counts 
                trans.x = MOVEDOWN - command;
            } else if ((command == MOVEUP) || (command == MOVEBACK)) {
                if (rotated[command - MOVEUP] == false)   { 
                    if (curBlock.rotates == true) {
                        deltaAngle += (command == MOVEUP ? 1 : 3);
                    } else {
                        moved = true;
                    }
                    rotated[command - MOVEUP] = true;
                }
            } else if (command == MOVEDOWN) {
                trans.y++;
            } else if (command == MOVEDROP) {
                if (dropped == false) {
                    curBlock.y += curBlock.rowsDropped;
                    placeBlock(curBlock);
                    curBlock = null;
                    dropped =  true;
                    return;
                }
            } else if (command == MOVEHOLD) {
                if ((held == false) && (holdUsed == false)) {
                    // get the next block by swapping
                    curBlock = null;
                    getNextBlock(true);
                    held = true;
                    return;
                }
            }        
        }
        // clear keys to accept new input
        moveDir.clear();

        // account for gravity's action on the block
        if (frame%GRAVITY == 0) 
            trans.y = 1;

        if (trans.x != 0) {
            // try to move the block right or left, if it is legal
            curBlock.x += trans.x;
            if (checkBlock(curBlock) != OK) {
                // the left and right movement is obstructed - move back
                curBlock.x -= trans.x;
            } else {
                // record the fact that this block moved
                moved = true;
            }
        }
        
        if (deltaAngle != 0) {
            // try to rotate, if needed
            curBlock.angle += deltaAngle;
            // move left or right to make room to rotate
            // trans.x will record how far we move
            trans.x = 0;
            while ((checkBlock(curBlock)%OVERLAP == LEFTEDGE) || (checkBlock(curBlock)%OVERLAP == RIGHTEDGE)) {
                if (checkBlock(curBlock)%OVERLAP == LEFTEDGE) {
                    // rotated off the left edge - move right to compensate
                    curBlock.x++;
                    trans.x++;
                } else {
                    // same on the right edge
                    curBlock.x--;
                    trans.x--;
                }
            }
            // now the block has been rotated away from the edge
            int check = checkBlock(curBlock);
            if ((check != OK) && (check%OVERLAP != TOPEDGE)) {
                // try to shoveaway from the obstruction, if we have shoveaways left
                if ((curBlock.shoveaways >= MAXSHOVEAWAYS) || (shoveaway(curBlock) == false)) {
                    curBlock.angle -= deltaAngle;
                    curBlock.x -= trans.x;
                } else {
                    // we've burned a shoveaway on this block
                    curBlock.shoveaways++;
                    moved = true;
                }
            } else if (check%OVERLAP == TOPEDGE) {
                // above the screen - try to move down after rotation
                int deltaY = 1;
                curBlock.y++;
                while (checkBlock(curBlock)%OVERLAP == TOPEDGE) {
                    deltaY++;
                    curBlock.y++;
                }
                // now check if the block is in a free position
                if (checkBlock(curBlock) ==  OK) {
                    moved = true;
                } else {
                    // revert to the original angle and x position
                    curBlock.angle -= deltaAngle;
                    curBlock.x -= trans.x;
                    curBlock.y -= deltaY;
                }
            } else {
                // record the fact that this block rotated
                moved = true;
            }
        }
       
        // if the block moved at all, its local sticking frames are reset
        // also, recalculate the number of squares this block can drop
        if (moved == true) {
            curBlock.localStickFrames = MAXLOCALSTICKFRAMES;
            curBlock.rowsDropped = calculateRowsDropped(curBlock);
        }

        if (curBlock.rowsDropped <= 0) {
            // block cannot drop - start to stick
            curBlock.globalStickFrames--;
            if (moved == false) 
                curBlock.localStickFrames--;
        } else {
            // the obstacle is no longer there - reset stick frames, and move down if required
            curBlock.globalStickFrames = MAXGLOBALSTICKFRAMES;
            curBlock.localStickFrames = MAXLOCALSTICKFRAMES;
            curBlock.y += trans.y;
            curBlock.rowsDropped -= trans.y;
        }

        // if the block has no stick frames left, place it down
        if ((curBlock.globalStickFrames <= 0) || (curBlock.localStickFrames <= 0)) {
            placeBlock(curBlock);
            curBlock = null;
        }
    }
    

    // the shoveaway is a desperate attempt to rotate the block around obstacles
    private boolean shoveaway(Block block) {
        int dir;

        // don't shoveaway a non-existent block
        if (block == null)
            return false;

        // attempt to rotate the block and possibly translate it
        for (int i = 0; i < 4; i++) {
            // the block can be shifted up to 2 units up in a shoveaway
            if (checkBlock(block) == OK) {
                return true;
            } else {
                // the block can also be shifted 1 unit left or right
                // to avoid giving preference to either direction, we decide randomly which one
                // to try first
                dir = 1 - 2*(1); // the 2*(1) should be a 2*(rand()%2) 
                block.x += dir;
                // if either direction works, we return the shoveaway
                if (checkBlock(block) == OK)
                    return true;
                block.x -= 2*dir;
                if (checkBlock(block) == OK)
                    return true;
                // otherwise, move back to center and shift up again 
                block.x += dir;

                if (i == 0) {
                    block.y++;
                } else if (i == 1) {
                    block.y -= 2;
                } else {
                    block.y--;
                }
            }
        }
        // at the end of the loop, the block has been moved up 3 squares - move it back down
        // no safe position was found, so the shoveaway fails
        block.y += 3;
        return false;
    }
    
    // place a block on the board, in its new fixed position
    private void placeBlock(Block block) {
        Point point = new Point();

        // log this event in multiplayer games
        if (gameMode == MULTIPLAYER) {
            events.add(PLACEBLOCK);
            events.add(block.x);
            events.add(block.y);
            events.add(block.angle);
        }

        // don't place a NULL block
        if (block == null)
            return;

        for (int i = 0; i < block.numSquares; i++) {
            // change square coordinates, from local coordinates into global
            if (block.angle%2 == 0) {
                // the block is rotated either 0 or 180 degrees
                point.x = block.x + block.squares[i].x*(1-(block.angle%4));
                point.y = block.y + block.squares[i].y*(1-(block.angle%4));
            } else {
                // the block is rotated either 90 or 270 degrees
                point.x = block.x + block.squares[i].y*((block.angle%4)-2);
                point.y = block.y + block.squares[i].x*(2-(block.angle%4));
            }
            board[point.x][point.y] = block.color[gameMode];
            blocksInRow[point.y]++;
            if (point.y < highestRow)
                highestRow = point.y;
            boardChanged = true;
        }

        // check if any rows have to be removed
        int rowsCleared = removeRows();
        if ((gameMode == MULTIPLAYER) && (rowsCleared > 0)) {
            // in a multiplayer game, log the appropriate attack and show an animation
            events.add(SENDATTACK);
            events.add(rowsCleared + combo - 1);
        }
    }
    
    private void getNextBlock() {
        getNextBlock(false);
    }

    private void getNextBlock(boolean swap) {
        int b;

        // log this event in multiplayer games, except when it is called from queueBlock
        // when curBlockType == -1, this method was called from queueBlock, which is already logged
        if (gameMode == MULTIPLAYER) {
            events.add(GETNEXTBLOCK);
            events.add(swap ? 1 : 0);
        }

        if ((swap == false) || (heldBlockType == -1)) {
            // get the first element from the preview list - it is the new block
            b = preview.remove(0);

            if (swap == true) {
                heldBlockType = curBlockType;
            } 
            // make the preview scroll to the next block
            previewAnim = PREVIEWANIMFRAMES;
            previewOffset = (blockData[b].height+1)*squareWidth/2;
        } else {
            // user swapped out block - do not change the preview list
            b = heldBlockType;
            // hold the current block
            heldBlockType = curBlockType;
        }

        // record the new block type
        curBlockType = b;

        curBlock = new Block();
        curBlock.x = blockData[b].x;
        curBlock.y = blockData[b].y - blockData[b].height + MAXBLOCKSIZE;
        curBlock.height = blockData[b].height;
        curBlock.numSquares = blockData[b].numSquares;
        oldBlock.numSquares = blockData[b].numSquares;
        for (int i = 0; i < curBlock.numSquares; i++) {
            curBlock.squares[i].x = blockData[b].squares[i].x;
            curBlock.squares[i].y = blockData[b].squares[i].y;
            
            oldBlock.squares[i].x = blockData[b].squares[i].x; 
            oldBlock.squares[i].y = blockData[b].squares[i].y;
        }
        curBlock.color = blockData[b].color;
        curBlock.rotates = blockData[b].rotates;

        curBlock.rowsDropped = calculateRowsDropped(curBlock);
        if (curBlock.rowsDropped < 0) {
            boardState = GAMEOVER;
            if (drawSprites) gameover.frameCol = 1;
        }
        
        if (swap == false) {
            // if we just generated a new block, we can hold again
            holdUsed = false;
        } else {
            holdUsed = true;
            justHeld = true;
        }
    }
    
    private int checkBlock(Block block) {
        Point point = new Point();
        int illegality = 0;
        int overlapsFound = 0;

        // don't check a non-existent block
        if (block == null)
            return OK;

        // run through each square to see if the block is in a legal position
        for (int i = 0; i < block.numSquares; i++) {
            // change square coordinates, from local coordinates into global
            if (block.angle%2 == 0) {
                // the block is rotated either 0 or 180 degrees
                point.x = block.x + block.squares[i].x*(1-(block.angle%4));
                point.y = block.y + block.squares[i].y*(1-(block.angle%4));
            } else {
                // the block is rotated either 90 or 270 degrees
                point.x = block.x + block.squares[i].y*((block.angle%4)-2);
                point.y = block.y + block.squares[i].x*(2-(block.angle%4));
            }
           
            if (point.y < 0) {
                // the highest priority errors are being off the top or bottom edge
                if (illegality == 0)
                    illegality = TOPEDGE;
            } else if (point.y >= NUMROWS) {
                // bottom edge - this can cause the block to stick
                if (illegality == 0) illegality = BOTTOMEDGE;
            } else if (point.x < 0) {
                // block is off the left edge of the board
                if (illegality == 0) illegality = LEFTEDGE;
            } else if (point.x >= NUMCOLS) {
                if (illegality == 0) illegality = RIGHTEDGE;
            } else if (board[point.x][point.y] != BLACK) {
                // keep track of the number of overlaps with blocks already placed
                overlapsFound++;
            }
        }

        // the flag returned contains all the information found
        // flag%OVERLAP gives any edges the block strayed over
        // flag/OVERLAP gives the number of overlaps
        // if flag == OK (OK = 0) then the position is legal
        return illegality + OVERLAP*overlapsFound;
    }    
    
    private int calculateRowsDropped(Block block) {
        if (block == null)
            return 0;

        for (int i = 0; i < NUMROWS+1; i++) {
            // check if the block is in a legal position
            if (checkBlock(block) == OK) {
                // still legal - move the block down 1 unit
                block.y++;
            } else {
                // the block is in illegal position - move it back, and
                // return the number of squares it can move down legally
                block.y -= i;
                return i-1;
            }
        }
        return NUMROWS;
    }
    
    // this method is called each time a block is placed - it clears any full rows
    private int removeRows() {
        int downShift = 0;

        for (int y = NUMROWS-1; y >= highestRow; y--) {
            if (blocksInRow[y] == NUMCOLS) {
                // downShift keeps track of the number of cleared rows up to this point
                downShift++;
            } else if (downShift > 0) {
                // down shift this row by downShift rows
                for (int x = 0; x < NUMCOLS; x++) {
                    board[x][y+downShift] = board[x][y];
                    blocksInRow[y+downShift] = blocksInRow[y];
                }
            }
        }
        // if any rows were removed, add empty space to the top of the board
        if (downShift > 0) {
            for (int y = highestRow; y < highestRow+downShift; y++) {
                for (int x = 0; x < NUMCOLS; x++) {
                    board[x][y] = BLACK;
                    blocksInRow[y] = 0;
                }
            }
            highestRow += downShift;
            score += ((1<<downShift)-1);
            combo++; 
        } else {
            combo = 0;
        }

        return downShift;
    }

    private void moveOldBlock() {
        oldBlock.x = curBlock.x;
        oldBlock.y = curBlock.y;
        oldBlock.angle = curBlock.angle;
        oldBlock.rowsDropped = curBlock.rowsDropped;
    }
    
    /** Drawing routines start here!                                *
     *  The only public method is draw(Graphics g, boolean redraw), *
     *  which calls the private methods drawBoard, drawGUI,         *
     *  drawBlock, etc.                                            **/

    public void draw(Graphics g, boolean redraw) {
        if (boardState == PLAYING) {
            if ((boardChanged) || redraw) {
                redrawBoard(g);
                boardChanged = false;
            } else {
                drawBoard(g);
            }    
            justHeld = false;
        } else if ((boardState >= PAUSED) && (boardState < GAMEOVER)) {
            if ((boardState == PAUSED) || redraw) {
                g.setColor(BLACK);
                g.fillRect(xPos, yPos, boardWidth, boardHeight);
                paused.draw(g);
                if (boardState == PAUSED) 
                    boardState++;
            }
        } else if ((boardState >= GAMEOVER) && (boardState < COUNTDOWN)) {
            if ((boardState == GAMEOVER) || redraw) {
                if ((gameMode == SINGLEPLAYER) || (!drawSprites)) {
                    redrawBoard(g, true);
                    if (drawSprites)
                        gameover.draw(g);
                    if (boardState == GAMEOVER)
                        boardState++;
                } else {
                    redrawBoard(g, true);
                    gameover.draw(g);
                    score = 0; 
                    boardState = COUNTDOWN;
                }    
            }    
        } else if (boardState >= COUNTDOWN) {
            if (redraw)
                redrawBoard(g, (drawSprites && (gameover.frameCol == 1)));
            if (drawSprites)  {
                if ((boardState%SECOND == COUNTDOWN) || redraw) {
                    gameover.frameRow = 1 + (boardState - COUNTDOWN)/SECOND;
                    gameover.draw(g);
                }
                boardState++;
                if (boardState == NUMSECONDS*SECOND + COUNTDOWN) 
                    resetBoard(false);
            }
        }
    }
    
    private void drawBoard(Graphics g) {
        if (justHeld == true) {
            int xOffset = oldBlock.x-blockData[heldBlockType].x;
            int yOffset = oldBlock.y-blockData[heldBlockType].y;

            drawBlock(g, blockData[heldBlockType], true, false, xOffset, yOffset, oldBlock.angle);
            drawBlock(g, blockData[heldBlockType], true, false, xOffset, yOffset+oldBlock.rowsDropped, oldBlock.angle);
        } else {
            drawBlock(g, oldBlock, true, false, 0, 0, 0);
            drawBlock(g, oldBlock, true, false, 0, oldBlock.rowsDropped, 0);
        }

        if (curBlock != null) {
            drawBlock(g, curBlock, false, true, 0, curBlock.rowsDropped);
            drawBlock(g, curBlock);
            moveOldBlock();
        }

        drawGUI(g);    
    }
    
    private void redrawBoard(Graphics g) {
        redrawBoard(g, false, RED);
    }

    private void redrawBoard(Graphics g, boolean tinted) {
        redrawBoard(g, tinted, RED); 
    }

    private void redrawBoard(Graphics g, boolean tinted, Color tint) {
        Color backColor, lineColor;

        backColor = BLACK;
        lineColor = mixedColor(WHITE, BLACK, LAMBDA);
        // on game over the screen reddens
        if (tinted == true) {
            backColor = mixedColor(tint, backColor, LAMBDA);
            lineColor = mixedColor(tint, lineColor, LAMBDA);
            highestRow = 0;
        }

        // first clear the board with black
        g.setColor(BLACK);
        g.fillRect(xPos, yPos, squareWidth*NUMCOLS, boardHeight);

        // draw in the vertical and horizontal grid lines
        g.setColor(lineColor);
        for (int i = 0; i < NUMCOLS; i++) {
            g.drawLine(xPos+squareWidth*i, yPos, xPos+squareWidth*i, yPos+boardHeight-1);
            g.drawLine(xPos+squareWidth*(i+1)-1, yPos, xPos+squareWidth*(i+1)-1, yPos+boardHeight-1);
        }
        for (int i = 0; i < NUMROWS-MAXBLOCKSIZE+1; i++) {
            g.drawLine(xPos, yPos+squareWidth*i, xPos+squareWidth*NUMCOLS-1, yPos+squareWidth*i);
            g.drawLine(xPos, yPos+squareWidth*(i+1)-1, xPos+squareWidth*NUMCOLS-1, yPos+squareWidth*(i+1)-1);
        }
        // below the highest row, fill in the colors of the blocks there
        for (int y = highestRow; y < NUMROWS; y++) {
            for (int x = 0; x < NUMCOLS; x++) {
                if (tinted == true) {
                    drawSquare(g, x, y, mixedColor(tint, board[x][y], 3*LAMBDA), (blocksInRow[y] == NUMCOLS));
                } else {
                    drawSquare(g, x, y, board[x][y], (blocksInRow[y] == NUMCOLS));
                }
            }
        }

        if (curBlock != null) {
            drawBlock(g, curBlock, false, true, 0, curBlock.rowsDropped);
            drawBlock(g, curBlock);
            moveOldBlock();
        }

        drawGUI(g, tinted, tint, true);
    }
    
    private void drawGUI(Graphics g) {
        drawGUI(g, false, RED, false);
    }

    private void drawGUI(Graphics g, boolean tinted, Color tint, boolean redraw) {
        int listY, digit, i;
        int x = 1;
        int yQueue = 5*(squareWidth/2)*(PREVIEW+2);

        // if the board is tinted, we erase the entire GUI right here
        if (tinted == true) {
            if (gameMode == SINGLEPLAYER) numbers.frameRow = 2;
            g.setColor(mixedColor(tint, BLACK, 2*LAMBDA));
            g.fillRect(xPos+squareWidth*NUMCOLS, yPos, boardWidth-squareWidth*NUMCOLS, boardHeight);
        } else {
            if (gameMode == SINGLEPLAYER) numbers.frameRow = 1;
            // otherwise, if the preview is scrolling, then we erase the GUI
            if (previewAnim > 0) {
                g.setColor(BLACK);
                g.fillRect(xPos+squareWidth*NUMCOLS, yPos, boardWidth-squareWidth*NUMCOLS, yQueue+1);
            }
        }

        // d acts like a y-Offset for the blocks - increases as we go down the preview
        if ((previewAnim > 0) || (tinted == true) || redraw) {
            Iterator<Integer> j = preview.iterator();
            
            int xOffset = squareWidth*NUMCOLS + sideBoard/2 - 3*squareWidth/4;
            listY = 0;
            if (previewAnim > 0) listY = (previewOffset*(previewAnim-1))/PREVIEWANIMFRAMES;
        
            int type;
            for (; j.hasNext();) {
                type = j.next();
                if (listY == 0) {
                    // the first one is drawn in a bright color
                    drawSmallBlock(g, blockData[type], xOffset, squareWidth+listY, squareWidth/2, -LAMBDA, tinted, tint);
                } else {
                    // all others are drawn in dull colors
                    drawSmallBlock(g, blockData[type], xOffset, squareWidth+listY, squareWidth/2, 2*LAMBDA, tinted, tint);
                }
                listY += (blockData[type].height+2)*squareWidth/2;
            }    
        }

        if ((holdUsed == true) || (previewAnim > 0) || (tinted == true) || redraw) {
            // the following code executes when the held piece changes 
            if (tinted == true) {
                g.setColor(mixedColor(tint, BLACK, 2*LAMBDA));
                g.fillRect(xPos+squareWidth*NUMCOLS, yPos+yQueue+1, 
                        boardWidth-squareWidth*NUMCOLS, boardHeight-yQueue-1);
            } else {
                g.setColor(BLACK);
                g.fillRect(xPos+squareWidth*NUMCOLS, yPos+yQueue+1, 
                        boardWidth-squareWidth*NUMCOLS, boardHeight-yQueue-1);    
            }
            drawHold(g, holdUsed, tinted, tint);
            
            previewAnim--;
            if (previewAnim == 0)
                previewOffset = 0;
        } else {
            // when the GUI isn't changing, other than the score, just clear a small rectangle where the score/attack queue will be drawn
            if (gameMode == SINGLEPLAYER) {
                g.setColor(BLACK);
                   g.fillRect(xPos+boardWidth-32-squareWidth/2, yPos+boardHeight-squareWidth/2-10, 32, 10);
            } else {
                g.setColor(BLACK);
                g.fillRect(xPos+squareWidth*NUMCOLS, yPos+boardHeight-squareWidth/2-6*ATTACKSIZE-1, 
                        boardWidth-squareWidth*NUMCOLS, 5*ATTACKSIZE);
            }
        }

        if (gameMode == SINGLEPLAYER) {
            // draw the score by picking the appropriate tiles from numbers
            numbers.x = xPos + boardWidth - squareWidth/2 - 8;
            numbers.y = yPos + boardHeight - squareWidth/2 - 10;
            for (i = 0; i < 4; i++) {
                if ((score >= x) || (i == 0)) {
                    digit = (score%(10*x))/x;
                    numbers.frameCol = digit+1;
                    numbers.draw(g);
                    numbers.x -= 8;
                }
                x = 10*x;
            }
        } else if (attacks.size() > 0) {
            int xOffset = boardWidth - squareWidth/2 - 6*ATTACKSIZE;
            int yOffset = boardHeight - squareWidth/2 - 6*ATTACKSIZE;
            Iterator<Integer> j = attacks.iterator();
            int attack = j.next();
            drawSmallBlock(g, blockData[attack + numBlocks + 27], xOffset + 9*ATTACKSIZE/2, yOffset, ATTACKSIZE, LAMBDA);
            int numDrawn = 1;

            for (; j.hasNext();) {
                attack = j.next();
                drawSmallBlock(g, blockData[attack + numBlocks + 27], xOffset, yOffset, ATTACKSIZE, 3*LAMBDA);
                numDrawn++;
                xOffset -= 9*ATTACKSIZE/2;
                if (xOffset < squareWidth*NUMCOLS + 3*ATTACKSIZE/2) {
                    if (numDrawn == attacks.size()-1) {
                        attack = j.next();
                        drawSmallBlock(g, blockData[attack + numBlocks + 27], xOffset, yOffset, ATTACKSIZE, 3*LAMBDA);
                    } else if (numDrawn < attacks.size()-1) {
                        drawSmallBlock(g, blockData[numBlocks + 70], xOffset, yOffset, ATTACKSIZE, 2*LAMBDA);
                    }
                    break;
                } 
            }
        }
    }

    private void drawHold(Graphics g, boolean shadow, boolean tinted, Color tint) {
        double lambda;
        
        if (shadow == true) {
            // draw the hold rectangle in the GUI to the right, in shadow - signifying that the hold has been used for this block 
            lambda = 3*LAMBDA;
        } else {
            // draw the hold rectangle in the GUI to the right in white 
            lambda = 0.0f;
        }
        int xOffset = xPos + squareWidth*NUMCOLS+squareWidth/2;
        int yOffset = yPos + 5*(squareWidth/2)*(PREVIEW+2)+1;
        if (tinted == true) {
            g.setColor(mixedColor(tint, WHITE, 4*LAMBDA));
            g.fillRect(xOffset, yOffset, 5*squareWidth/2, 4*squareWidth);
            g.setColor(mixedColor(tint, BLACK, 2*LAMBDA));
            g.fillRect(xOffset+1, yOffset+1, 5*squareWidth/2-2, 4*squareWidth-2);
        } else {
            g.setColor(mixedColor(BLACK, WHITE, lambda));
            g.fillRect(xOffset, yOffset, 5*squareWidth/2, 4*squareWidth);
            g.setColor(BLACK);
            g.fillRect(xOffset+1, yOffset+1, 5*squareWidth/2-2, 4*squareWidth-2);
        }
        
        if (heldBlockType != -1) { 
            xOffset = squareWidth*NUMCOLS + sideBoard/2 - 3*squareWidth/4;
            yOffset += -yPos + 2*squareWidth - ((squareWidth/2)*(blockData[heldBlockType].height))/2;
            drawSmallBlock(g, blockData[heldBlockType], xOffset, yOffset, (squareWidth/2), lambda, tinted);
        }
    }

    private void drawBlock(Graphics g, Block block) {
        drawBlock(g, block, false, false, 0, 0, 0);
    }

    private void drawBlock(Graphics g, Block block, boolean erase, boolean shadow,
            int xOffset, int yOffset) {
        drawBlock(g, block, erase, shadow, xOffset, yOffset, 0);
    }

    private void drawBlock(Graphics g, Block block, boolean erase, boolean shadow, 
            int xOffset, int yOffset, int aOffset) {
        Point point = new Point();

        // don't draw a non-existent block
        if (block == null)
            return;

        // draw a block, square by square
        for (int i = 0; i < block.numSquares; i++) {
            if ((block.angle+aOffset)%2 == 0) {
                // either the block is unrotated, or rotated 180 degrees - x's correspond to x's
                point.x = block.x + block.squares[i].x*(1-((block.angle+aOffset)%4));
                point.y = block.y + block.squares[i].y*(1-((block.angle+aOffset)%4));
            } else {
                // the block is rotated 90 or 270 degrees - x's in local coordinates are y's in global coords
                point.x = block.x + block.squares[i].y*(((block.angle+aOffset)%4)-2);
                point.y = block.y + block.squares[i].x*(2-((block.angle+aOffset)%4));
            }

            if ((point.x+xOffset >= 0) && (point.x+xOffset < NUMCOLS) && 
                    (point.y+yOffset >= 0) && (point.y+yOffset < NUMROWS)) {
                // draw the block at its correct position
                // active blocks are drawn in a lighter color than placed blocks 
                if (shadow == false) {
                    if (erase == false) {
                        drawSquare(g, point.x+xOffset, point.y+yOffset, 
                                mixedColor(WHITE, block.color[gameMode], LAMBDA*(1-LAMBDA)));
                    } else {
                        drawSquare(g, point.x+xOffset, point.y+yOffset, board[point.x+xOffset][point.y+yOffset]);
                    }
                } else {
                    drawSquare(g, point.x+xOffset, point.y+yOffset, block.color[gameMode], true);
                }
            }
        }
    }
    
    private void drawSmallBlock(Graphics g, Block block, int xOffset, int yOffset, int width, double lambda) {
        drawSmallBlock(g, block, xOffset, yOffset, width, lambda, false, RED);
    }

    private void drawSmallBlock(Graphics g, Block block, int xOffset, int yOffset, int width, double lambda, 
            boolean tinted) {
        drawSmallBlock(g, block, xOffset, yOffset, width, lambda, tinted, RED);
    }

    private void drawSmallBlock(Graphics g, Block block, int xOffset, int yOffset, 
            int width, double lambda, boolean tinted, Color tint) {
        Point point = new Point();
        Point pos = new Point();
        Point dim = new Point();
        boolean checkered;
        Color color;
        
        // don't draw a non-existent block
        if (block == null)
            return;

        // draw a block, square by square
        for (int i = 0; i < block.numSquares; i++) {
            if (block.angle%2 == 0) {
                // either the block is unrotated, or rotated 180 degrees - x's correspond to x's
                point.x = block.x + block.squares[i].x*(1-(block.angle%4));
                point.y = block.y + block.squares[i].y*(1-(block.angle%4));
            } else {
                // the block is rotated 90 or 270 degrees - x's in local coordinates are y's in global coords
                point.x = block.x + block.squares[i].y*((block.angle%4)-2);
                point.y = block.y + block.squares[i].x*(2-(block.angle%4));
            }
            checkered = ((point.x + point.y) % 2) == 1;

            pos.x = xPos + xOffset + (point.x-NUMCOLS/2+1)*width;
            pos.y = yPos + yOffset + point.y*width;
            dim.x = width;
            dim.y = width;

            if (tinted) {
                color = mixedColor(tint, mixedColor(BLACK, block.color[gameMode], lambda), 3*LAMBDA);
            } else {
                color = mixedColor(BLACK, block.color[gameMode], lambda);
            }
            if (checkered)
                color = mixedColor(BLACK, color, 0.6*LAMBDA);

            g.setColor(color);
            g.fillRect(pos.x, pos.y, dim.x, dim.y);
        }
    }

    private void drawSquare(Graphics g, int x, int y, Color color) {
        drawSquare(g, x, y, color, false);
    }

    private void drawSquare(Graphics g, int x, int y, Color color, boolean shadow) {
        Point pos = new Point();
        Point dim = new Point();
      
        // don't draw the first MAXBLOCKSIZE-1 rows
        // shift the other rows up
        y -= MAXBLOCKSIZE-1;
        if (y < 0)
            return;

        // draws a specific square
        // first position the drawing square around the border
        pos.x = xPos + squareWidth*x;
        pos.y = yPos + squareWidth*y;
        dim.x = squareWidth;
        dim.y = squareWidth;

        // draw the square's border, a mix of BLACK and the square's color
        if (shadow == false) {
            g.setColor(mixedColor(WHITE, color, LAMBDA));
            g.fillRect(pos.x, pos.y, dim.x, dim.y); 
        } else {
            // draw the gray border around a shadowed square
            g.setColor(mixedColor(WHITE, BLACK, LAMBDA));
            g.fillRect(pos.x, pos.y, dim.x, dim.y); 
        }

        // shrink the square by 1 on all four sides
        pos.x += 1;
        pos.y += 1;
        dim.x -= 2;
        dim.y -= 2;

        // draw the interior of the square in the color board[x][y]
        if (shadow == false) {
            g.setColor(color);
            g.fillRect(pos.x, pos.y, dim.x, dim.y); 
        } else {
            g.setColor(BLACK);
            g.fillRect(pos.x, pos.y, dim.x, dim.y);  
        }

        if (shadow == true) {
            // draw a sequence of diagonal lines to represent the shadow
            g.setColor(color);
            for (int i = 0; i < 2*squareWidth-1; i++) {
                if (((squareWidth*(x+y))+i)%4 == 0) {
                    if (i < squareWidth) {
                        g.drawLine(xPos+squareWidth*x, yPos+squareWidth*y+i, 
                                xPos+squareWidth*x+i, yPos+squareWidth*y);
                    } else {
                        g.drawLine(xPos+squareWidth*(x+1)-1, yPos+squareWidth*(y-1)+i+1, 
                                xPos+squareWidth*(x-1)+i+1, yPos+squareWidth*(y+1)-1);
                    }
                }
            }    
        }    
    }
}
