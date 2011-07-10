package ntris_src;

import static ntris_src.Constants.*;
import java.awt.event.*;

public class KeyRepeater implements KeyListener {
	private int pause, repeat;
	private boolean[] keyDown = new boolean[NUMKEYS];
	private long[] nextFireTime = new long[NUMKEYS];
	private RepeatHandler handler;
	
	public static interface RepeatHandler {
		public void repeaterPress(int key);
		public void repeaterRelease(int key);
	}
	
	public KeyRepeater(RepeatHandler handler, int pause, int repeat) {
		this.handler = handler;
		this.pause = pause;
		this.repeat = repeat;

		for (int i = 0; i < NUMKEYS; i++) {
			keyDown[i] = false;
			nextFireTime[i] = -1;
		}
	}
	
	private int keyCode(int key) {
		switch (key) {
		case KeyEvent.VK_ESCAPE:
			return ESCAPE;
		case KeyEvent.VK_UP:
			return MOVEUP;
		case KeyEvent.VK_DOWN:
			return MOVEDOWN;
		case KeyEvent.VK_LEFT:
			return MOVELEFT;
		case KeyEvent.VK_RIGHT:
			return MOVERIGHT;
		case KeyEvent.VK_SPACE:
			return MOVEDROP;
		case KeyEvent.VK_SHIFT:
			return MOVEHOLD;
		case KeyEvent.VK_Z:
			return MOVEBACK;
		case KeyEvent.VK_X:
			return MOVEUP;
		case KeyEvent.VK_C:
			return MOVEHOLD;
		case KeyEvent.VK_ENTER:
			return ENTER;
		case KeyEvent.VK_P:
			return PAUSE;
		default:
			return -1;
		}
	}
	
	public void keyPressed(KeyEvent e) {
		int key = keyCode(e.getKeyCode());
		if (key >= 0) keyDown[key] = true;
	}

	public void keyReleased(KeyEvent e) {
		int key = keyCode(e.getKeyCode());
		if (key >= 0) keyDown[key] = false;
	}

    public void releaseAll() {
        for (int i = 0; i < NUMKEYS; i++)
            keyDown[i] = false;
	}
	
	public void keyTyped(KeyEvent e) { }

	public void query() {
		query(System.currentTimeMillis());
	}

	public void query(long curTime) {
		for (int i = 0; i < NUMKEYS; i++) {
			if (keyDown[i]) {
				if (nextFireTime[i] < 0) {
					handler.repeaterPress(i);
					nextFireTime[i] = curTime + pause;
				} else if (curTime > nextFireTime[i]) {
					handler.repeaterPress(i);
					nextFireTime[i] = curTime + repeat;
				}
			} else if (nextFireTime[i] > 0) {
				handler.repeaterRelease(i);
				nextFireTime[i] = -1;
			}
		}
	}

    public void reset() {
        for (int i = 0; i < NUMKEYS; i++) {
            keyDown[i] = false;
            nextFireTime[i] = -1;
        }
    }
}
