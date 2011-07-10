package ntris_src;

import static ntris_src.Constants.*;
import static ntris_src.ntrisColor.*;
import java.awt.Color;

public class Block {
	public int x, y;
	public int angle;
	public int numSquares;
	public Point[] squares = new Point[MAXBLOCKSIZE];
	public Color[] color = new Color[2];
	public int shoveaways;
	public int localStickFrames;
	public int globalStickFrames;
	public int height;
	public boolean rotates;
	public int rowsDropped;
	
	public Block() {
		x = 0;
		y = 0;
		angle = 0;
		numSquares = 0;
		for (int i = 0; i < MAXBLOCKSIZE; i++) {
			squares[i] = new Point();
		}
		color[0] = BLUE;
		color[1] = RED;
		shoveaways = 0;
		localStickFrames = MAXLOCALSTICKFRAMES;
		globalStickFrames = MAXGLOBALSTICKFRAMES;
		rotates = true;
		height = 0;
	}

    // returns the index of the block which is shaped like a given ASCII character
    public static int charToSymbol(char c) {
        if (Character.isLetter(c)) {
            return Character.toLowerCase(c) - 'a';
        } else if (Character.isDigit(c)) {
            return 27 + Character.digit(c, 10);
        } else {
            switch (c) {
            case ('`'): return 26; 
            case ('-'): return 37; 
            case ('='): return 38; 
            case ('~'): return 39; 
            case ('!'): return 40; 
            case ('@'): return 41; 
            case ('#'): return 42; 
            case ('$'): return 43; 
            case ('%'): return 44; 
            case ('^'): return 45; 
            case ('&'): return 46; 
            case ('*'): return 47; 
            case ('('): return 48; 
            case (')'): return 49; 
            case ('_'): return 68; 
            case ('+'): return 51; 
            case ('['): return 52; 
            case (']'): return 53; 
            case ('\\'): return 54; 
            case ('{'): return 55; 
            case ('}'): return 56; 
            case ('|'): return 57; 
            case (';'): return 58; 
            case ('\''): return 59; 
            case (':'): return 60; 
            case ('"'): return 61; 
            case (','): return 62; 
            case ('.'): return 63; 
            case ('/'): return 64; 
            case ('<'): return 65; 
            case ('>'): return 66; 
            case ('?'): return 67; 
            case (' '): return 68; 
            default: {
                    return -1; 
                }   
            }   
        }   
    }   
}
