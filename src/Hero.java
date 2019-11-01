import javax.swing.*;
import java.applet.Applet;
import java.applet.AudioClip;
import java.awt.*;
import java.util.Random;

public class Hero extends Sprite {

	AudioClip hurtSound = null;
	Random random;
	protected Image walk1;
	protected Image walk2;
	protected Image heart;
	public int check = 0, pWidth, pHeight, lives = 3, score = 0;
	public boolean walking = false;
	private boolean win = false;

	public Hero(JFrame f,
				int x, int y, int dx, int dy,
				int xSize, int ySize, int w, int h,
				String filename) {
		super(f, x, y, dx, dy, xSize, ySize, filename);
		pWidth = w;
		pHeight = h;
		random = new Random();
		setPosition();
		loadClips();

		walk1 = loadImage ("assets/images/weapon8.png");
		walk2 = loadImage ("assets/images/weapon9.png");
		heart = loadImage ("assets/images/heart.png");
	}

	public void setPosition() {
		int x = 200;
		setX(x);
	}

	public void update() {

	}

	public void damagePlayer(){
		lives--;
		playClip(1);

	}

	public void drawLives(Graphics g){
		if (lives == 3){
			g.drawImage(heart, 20, 20, 40, 40, null);
			g.drawImage(heart, 70, 20, 40, 40, null);
			g.drawImage(heart, 120, 20, 40, 40, null);
		}

		if (lives == 2){
			g.drawImage(heart, 20, 20, 40, 40, null);
			g.drawImage(heart, 70, 20, 40, 40, null);
		}

		if (lives == 1){
			g.drawImage(heart, 20, 20, 40, 40, null);
		}
	}


	public void draw (Graphics g) {

		if (lives == 0){
			return;
		}

		if (score == 400){
			win = true;
		}

		drawLives(g);

		if (walking)
			walk(g);
		else
			g.drawImage(walk1, x, y, xSize, ySize, null);

		try {
			Thread.sleep(70);
		} catch (InterruptedException e) {
		}

	}

	public void walk(Graphics g){
		if (check == 0){
			g.drawImage(walk1, x, y, xSize, ySize, null);
			check = 1;
			return;
		}
		if (check == 1){
			g.drawImage(walk2, x, y, xSize, ySize, null);
			check = 0;
		}
	}

	public void jump(){
		if (!window.isVisible ()) return;
		System.out.println("JUMPING " + y);

		while (y > pHeight/2){
			y -= dy;
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
			}
		}

		while (y < pHeight-170){
			y += dy;
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
			}
		}

	}

	public void moveLeft () {

		if (!window.isVisible ()) return;

		x = x - dx;

		if (x < 0) {					// hits left wall
			x = 0;
		}
	}

	public void moveRight () {

		if (!window.isVisible ()) return;

		x = x + dx;

		if (x + xSize >= dimension.width) {		// hits right wall
			x = dimension.width - xSize;
		}



	}

	public void loadClips() {

		try {
			hurtSound = Applet.newAudioClip (
						getClass().getResource("assets/sounds/hurt.wav"));
		}
		catch (Exception e) {
			System.out.println ("Error loading sound file: " + e);
		}

	}

	public void playClip(int index) {

		if (index == 1 && hurtSound != null)
			hurtSound.play();
	}

}