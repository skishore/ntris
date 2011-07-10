package ntris_src;

import java.awt.Color;

public class ntrisColor {
	// Color constants
	public static final double LAMBDA = 0.20;
	
	public static final Color BLACK = new Color(0x00000000);
	public static final Color GRAY = new Color(0x00ffffff);
	public static final Color WHITE = new Color(0x00ffffff);
	public static final Color RED = new Color(0x00ff0000);
	public static final Color LIME = new Color(0x0000ff00);
	public static final Color BLUE = new Color(0x000000ff);
	public static final Color CYAN = new Color(0x0000ffff);
	public static final Color PURPLE = new Color(0x00800080);
	public static final Color YELLOW = new Color(0x00ffff00);
	public static final Color ORANGE = new Color(0x00ffa500);
	public static final Color DARKORANGE = new Color(0x00ff8c00);
	public static final Color ORANGERED = new Color(0x00ff4500);
	public static final Color TAN = new Color(0x00d2b48c);
	public static final Color SALMON = new Color(0x00fa8072);
	public static final Color DARKRED = new Color(0x00b21111);
	public static final Color PURPLERED = new Color(0x008b0011);
	public static final Color XLIGHTBLUE = new Color(0x0087ceeb);
	public static final Color LIGHTBLUE = new Color(0x004169e1);
	public static final Color PURPLEBLUE = new Color(0x0000008b);
	public static final Color HOTPINK = new Color(0x00ff00ff);
	public static final Color PLUM = new Color(0x00dda0dd);
	public static final Color ORCHID = new Color(0x00da70d6);
	public static final Color DARKPINK = new Color(0x009966cc);
	public static final Color TURQUOISE = new Color(0x0048d1cc);
	public static final Color DARKGREEN = new Color(0x0020b2aa);
	public static final Color GREEN = new Color(0x003cb371);
	public static final Color LIGHTGREEN = new Color(0x0098fb98);
	public static final Color XXXLIGHTGRAY = new Color(0x00dddddd);
	public static final Color XXLIGHTGRAY = new Color(0x00cccccc);
	public static final Color XLIGHTGRAY = new Color(0x00bbbbbb);
	public static final Color LIGHTGRAY = new Color(0x00aaaaaa);
	public static final Color GOLD = new Color(0x00ffd700);
	public static final Color STAIRCASE = new Color(0x00b8860b);

    // the light rainbow colors that we finally settled on
    public static Color colorCode(int index) {
        Color color = rainbowCode(index);
        return mixedColor(WHITE, color, 3.2*LAMBDA);
    }

	// the pale blue colors that some people prefer game
	public static Color washedOutCode(int index) {
		Color color = rainbowCode(index);
		return new Color(color.getGreen(), color.getRed(), 255);
	}

    // the difficulty coloring of blocks in multiplayer games
    public static Color difficultyColor(Color oldColor, int level, int maxLevel) {
        int blue = oldColor.getBlue();
        int green = oldColor.getGreen();
        int red = oldColor.getRed();
    
        if (level > 0) {
            Color blueShift = new Color(green/2, red/2, 255);
            double lambda = (0.8f + (1.0f*level)/maxLevel)/2.0f;
            return mixedColor(WHITE, mixedColor(RED, blueShift, lambda), LAMBDA);
        } else {
            return mixedColor(WHITE, new Color(green/4, red/2, (255 + blue)/2), LAMBDA);
        }
    }

    // linearly interpolates between two colors
    public static Color mixedColor(Color clr1, Color clr2, double lambda) {
		int blue = (int) (clr1.getBlue()*lambda + clr2.getBlue()*(1-lambda));
		int green = (int) (clr1.getGreen()*lambda + clr2.getGreen()*(1-lambda));
		int red = (int) (clr1.getRed()*lambda + clr2.getRed()*(1-lambda));
		
		blue = Math.min(Math.max(blue, 0), 255);
		green = Math.min(Math.max(green, 0), 255);
		red = Math.min(Math.max(red, 0), 255);
		
		return new Color(red, green, blue);
	}
	
	// takes an index and returns the corresponding color
	public static Color rainbowCode(int index) {
	    switch (index) {
	        case 0:
	            return WHITE;
	        case 1:
	            return XXXLIGHTGRAY;
	        case 2:
	            return XXLIGHTGRAY;
	        case 3:
	            return YELLOW;
	        case 4:
	            return XLIGHTGRAY;
	        case 5:
	            return XLIGHTBLUE;
	        case 6:
	            return SALMON;
	        case 7:
	            return PLUM;
	        case 8:
	            return GOLD;
	        case 9:
	            return ORCHID;
	        case 10:
	            return LIGHTGREEN;
	        case 11:
	            return LIGHTGRAY;
	        case 12:
	            return LIGHTBLUE;
	        case 13:
	            return RED;
	        case 14:
	            return BLUE;
	        case 15:
	            return DARKRED;
	        case 16:
	            return PURPLERED;
	        case 17:
	            return PURPLEBLUE;
	        case 18:
	            return HOTPINK;
	        case 19:
	            return PURPLE;
	        case 20:
	            return TAN;
	        case 21:
	            return DARKORANGE;
	        case 22:
	            return DARKGREEN;
	        case 23:
	            return STAIRCASE;
	        case 24:
	            return ORANGERED;
	        case 25:
	            return TURQUOISE;
	        case 26:
	            return DARKPINK;
	        case 27:
	            return ORANGE;
	        case 28:
	            return GREEN;
	        default:
	        	return RED;
	    }
	}	
}
