package ntris_src;

import java.applet.*;
import java.awt.*;
import java.net.*;

public class Sprite {
    public int x;
    public int y;
    public int frameCol;
    public int frameRow;

    private Applet canvas;
    private int height;
    private int width;
    private Image spriteImage;
    private Image backImage;
    private Graphics biGraphics;
    
    public Sprite(Applet applet, int w, int h, String imageName, 
    		int numCols, int numRows, MediaTracker mediaTracker, int ID) {
        canvas = applet;
		width = w;
        height = h;
        x = 0;
        y = 0;
        frameCol = 1;
        frameRow = 1;
        
		URL url = this.getClass().getResource(imageName); 
        spriteImage = Toolkit.getDefaultToolkit().getImage(url);
        mediaTracker.addImage(spriteImage, ID);
        backImage = canvas.createImage(width, height);
        biGraphics = backImage.getGraphics();
    }

    public void erase(Graphics g) {
        g.drawImage(backImage, x, y, x+width, y+height,
        		0, 0, width, height, canvas);
    } 

    public void saveUnder(Image i) {
        biGraphics.drawImage(i, 0, 0, width, height,
        		x, y, x+width, y+height, canvas);
    } 

    public void draw(Graphics g) {
        g.drawImage(spriteImage, x, y, x+width, y+height,
        		width*(frameCol-1), height*(frameRow-1), width*frameCol, height*frameRow, canvas);
    }
} 