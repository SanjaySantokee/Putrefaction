import javax.swing.*;
import java.applet.Applet;
import java.applet.AudioClip;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferStrategy;
import java.util.Random;

public class GameFrame extends JFrame implements Runnable, KeyListener {
  	private static final int NUM_BUFFERS = 2;	// used for page flipping

	private int pWidth, pHeight;     		// dimensions of screen

	private Thread gameThread = null;            	// the thread that controls the game
	private volatile boolean running = false;    	// used to stop the animation thread

	private Hero hero;
    private Rat rat = null;
	private Image bgImage;				// background image
	private Image mountains;
	private Image rocks;
	private Image grass;
	AudioClip playSound = null;			// theme sound

	private Rectangle2D.Double enemyBoundary;
	private boolean isOverEnemy = false;

	private Rectangle2D.Double masterBoundary;
	private boolean isOverMaster = false;

  	// used at game termination
	private boolean finishedOff = false;

	// used by the quit 'button'
	private volatile boolean isOverQuitButton = false;
	private Rectangle quitButtonArea;

	// used by the pause 'button'
	private volatile boolean isOverPauseButton = false;
	private Rectangle pauseButtonArea;
	private volatile boolean isPaused = false;

	// used by the stop 'button'
	private volatile boolean isOverStopButton = false;
	private Rectangle stopButtonArea;
	private volatile boolean isStopped = false;

	// used by the show animation 'button'
	private volatile boolean isOverShowAnimButton = false;
	private Rectangle showAnimButtonArea;
	private volatile boolean isAnimShown = false;

	// used by the pause animation 'button'
	private volatile boolean isOverPauseAnimButton = false;
	private Rectangle pauseAnimButtonArea;
	private volatile boolean isAnimPaused = false;
  
	// used for full-screen exclusive mode  
	private GraphicsDevice device;
	private Graphics gScr;
	private BufferStrategy bufferStrategy;

	// moves mountain background
	private int mountainX1 = 0, mountainX2 = 0, rocksX = 0, rocksX2 = 0, grassX1 = 0, grassX2 = 0;
	private Random random;
	private boolean isWinner = false;

	private static GameFrame instance;


	private GameFrame () {
		super("The Putrefaction");

		initFullScreen();

		// create game sprites

		hero = new Hero(this, 0, pHeight-170, 7, 2, 150, 110, getBounds().width, getBounds().height, "assets/images/bat.gif");
		rat = new Rat(this, hero, 0, 0, -20, 10, getBounds().width, getBounds().height, 150, 110, "assets/images/weapon8.png");

		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				testMousePress(e.getX(), e.getY());
				damageEnemy(e.getX(), e.getY());
			}
		});

		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseMoved(MouseEvent e) {
				testMouseMove(e.getX(), e.getY()); 
			}
		});

		addKeyListener(this);			// respond to key events

		// specify screen areas for the buttons
		//  leftOffset is the distance of a button from the left side of the window

		int leftOffset = (pWidth - (5 * 150) - (4 * 20)) / 2;
//		pauseButtonArea = new Rectangle(leftOffset, pHeight-60, 150, 40);

		leftOffset = leftOffset + 170;
//		stopButtonArea = new Rectangle(leftOffset, pHeight-60, 150, 40);

		leftOffset = leftOffset + 170;
//		showAnimButtonArea = new Rectangle(leftOffset, pHeight-60, 150, 40);

		leftOffset = leftOffset + 170;
//		pauseAnimButtonArea = new Rectangle(leftOffset, pHeight-60, 150, 40);

		leftOffset = leftOffset + 170;
		quitButtonArea = new Rectangle(leftOffset, pHeight-60, 150, 40);

		loadImages();
		loadClips();
		startGame();
	}

	public static GameFrame getInstance(){

		if (instance == null){
			instance = new GameFrame();
		}
		return instance;

	}

	private void initFullScreen() {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		device = ge.getDefaultScreenDevice();

		setUndecorated(true);	// no menu bar, borders, etc.
		setIgnoreRepaint(true);	// turn off all paint events since doing active rendering
		setResizable(false);	// screen cannot be resized
		
		if (!device.isFullScreenSupported()) {
			System.out.println("Full-screen exclusive mode not supported");
			System.exit(0);
		}

		device.setFullScreenWindow(this); // switch on full-screen exclusive mode

		// we can now adjust the display modes, if we wish

		showCurrentMode();

		pWidth = getBounds().width;
		pHeight = getBounds().height;

		try {
			createBufferStrategy(NUM_BUFFERS);
		}
		catch (Exception e) {
			System.out.println("Error while creating buffer strategy " + e); 
			System.exit(0);
		}

		bufferStrategy = getBufferStrategy();
	}

	// this method creates and starts the game thread

    private void createCursor(){
	    Toolkit tookit = Toolkit.getDefaultToolkit();
	    Image img = getToolkit().getImage("assets/images/Cursor_Attack.png");
	    Point point = new Point(0, 0);
	    Cursor cursor = tookit.createCustomCursor(img, point, "Cursor");
	    setCursor(cursor);
    }

	private void startGame() {
	    createCursor();
        mountainX2 = getBounds().width;
        rocksX2 = getBounds().width;
        grassX2 = getBounds().width;

        if (gameThread == null || !running) {
			gameThread = new Thread(this);
			gameThread.start();
            rat.setDeviceResolution(getBounds().width, getBounds().height);
			mountainX2 = getBounds().width;
			playSound.loop();
		}
	}
    
	/* This method handles mouse clicks on one of the buttons
	   (Pause, Stop, Show Anim, Pause Anim, and Quit).
	*/


	private void damageEnemy(int x, int y){
		if (running) {
			isOverEnemy = enemyBoundary.contains(x,y) ? true : false;
		}

		if (isOverEnemy){
			rat.damage();
			System.out.println("Score:" + hero.score);
		}
	}

	private void testMousePress(int x, int y) {

		if (isStopped && !isOverQuitButton) 	// don't do anything if game stopped
			return;

		if (isOverStopButton) {			// mouse click on Stop button
			isStopped = true;
			isPaused = false;
		}
		else
		if (isOverPauseButton) {		// mouse click on Pause button
			isPaused = !isPaused;     	// toggle pausing
		}
		else
		if (isOverShowAnimButton && !isPaused) {// mouse click on Show Anim button
			if (isAnimShown)		// make invisible if visible
		 		isAnimShown = false;
			else {				// make visible if invisible
				isAnimShown = true;
				isAnimPaused = false;	// always animate when making visible
			}
		}
		else
		if (isOverPauseAnimButton) {		// mouse click on Pause Anim button
			isAnimPaused = !isAnimPaused;	// toggle pausing
		}
		else if (isOverQuitButton) {		// mouse click on Quit button
			running = false;		// set running to false to terminate
		}
  	}


	/* This method checks to see if the mouse is currently moving over one of
	   the buttons (Pause, Stop, Show Anim, Pause Anim, and Quit). It sets a
	   boolean value which will cause the button to be displayed accordingly.
	*/

	private void testMouseMove(int x, int y) { 
		if (running) {
//			isOverPauseButton = pauseButtonArea.contains(x,y) ? true : false;
//			isOverStopButton = stopButtonArea.contains(x,y) ? true : false;
//			isOverShowAnimButton = showAnimButtonArea.contains(x,y) ? true : false;
//			isOverPauseAnimButton = pauseAnimButtonArea.contains(x,y) ? true : false;
			isOverQuitButton = quitButtonArea.contains(x,y) ? true : false;

		}
	}

	// implementation of KeyListener interface

	public void keyPressed (KeyEvent e) {


		int keyCode = e.getKeyCode();
         
		if ((keyCode == KeyEvent.VK_ESCAPE) || (keyCode == KeyEvent.VK_Q) ||
             	   (keyCode == KeyEvent.VK_END)) {
           		running = false;		// user can quit anytime by pressing
			return;				//  one of these keys (ESC, Q, END)
         	}	

		if (hero == null || isPaused || isStopped)
			return;

		if (keyCode == KeyEvent.VK_SPACE) {
			hero.jump();
		}

		if (keyCode == KeyEvent.VK_A) {
			hero.moveLeft();
		}
		else
		if (keyCode == KeyEvent.VK_D) {
			random = new Random();
			int x = 0;

		    if (hero.getX() < 700){
                hero.moveRight();
                hero.walking = true;
				x = (int)(Math.random() * 60 + 50);
				x = x * -1;
				rat.setDX(x);
            }

		    if (hero.getX() >= 700){
		    	hero.walking = true;
				x = (int)(Math.random() * 100 + 90);
				x = x * -1;
				rat.setDX(x);
				mountainScroll();
                rocksScroll();
                grassScroll();
            }

		}
	}

	public void keyReleased (KeyEvent e) {
		int keyCode = e.getKeyCode();

		if (keyCode == KeyEvent.VK_D) {
			hero.walking = false;
			rat.setDX(-55);
		}
	}

	public void keyTyped (KeyEvent e) {

	}

	public void mountainScroll(){
        if (mountainX1 + pWidth >= 0)
            mountainX1-=2;
        else
            mountainX1 = pWidth-2;

        if (mountainX2 + pWidth >= 0)
            mountainX2-=2;
        else
            mountainX2 = pWidth-2;
    }

    public void rocksScroll(){
        if (rocksX + pWidth >= 0)
            rocksX-=15;
        else
            rocksX = pWidth-15;

        if (rocksX2 + pWidth >= 0)
            rocksX2-=15;
        else
            rocksX2 = pWidth-15;
    }

    public void grassScroll(){
        if (grassX1 + pWidth >= 0)
            grassX1-=20;
        else
            grassX1 = pWidth-20;

        if (grassX2 + pWidth >= 0)
            grassX2-=20;
        else
            grassX2 = pWidth-20;
    }


	// implmentation of MousePressedListener interface

	// implmentation of MouseMotionListener interface

	// The run() method implements the game loop.

	public void run() {

		running = true;
		try {
			while (running) {

	  			gameUpdate();     
	  			screenUpdate();
				Thread.sleep(10);

			}
		}
		catch(InterruptedException e) {};

		finishOff();
	}


	// This method updates the game objects (animation and ball)

	private void gameUpdate() { 

		if (!isPaused) {
			if (!isStopped)
				rat.update();
		}
  	}


	// This method updates the screen using double buffering / page flipping

	private void screenUpdate() { 

		try {
			gScr = bufferStrategy.getDrawGraphics();
			if (hero.score >= 400)
				win(gScr);
			else if (hero.lives <= 0)
				gameOverRender(gScr);
			else
				gameRender(gScr);
			gScr.dispose();
			if (!bufferStrategy.contentsLost())
				bufferStrategy.show();
			else
				System.out.println("Contents of buffer lost.");
      
			// Sync the display on some systems.
			// (on Linux, this fixes event queue problems)

			Toolkit.getDefaultToolkit().sync();
		}
		catch (Exception e) { 
			e.printStackTrace();  
			running = false; 
		} 
	}

	/* This method renders all the game entities to the screen: the
	   background image, the buttons, ball, bat, and the animation.
	*/

	private void gameOverRender(Graphics gScr){
		gScr.drawImage (bgImage, 0, 0, pWidth, pHeight, null);
		gameOverMessage(gScr);
	}


	private void win(Graphics gScr) {
		gScr.drawImage (bgImage, 0, 0, pWidth, pHeight, null);

		Font font = new Font("SansSerif", Font.BOLD, 24);
		FontMetrics metrics = this.getFontMetrics(font);

		String msg = "You Win! Press Escape to Close Game";

		int x = (pWidth - metrics.stringWidth(msg)) / 2;
		int y = (pHeight - metrics.getHeight()) / 2;

		gScr.setColor(Color.BLUE);
		gScr.setFont(font);
		gScr.drawString(msg, x, y);
	}

	private void gameRender(Graphics gScr){
 
		gScr.drawImage (bgImage, 0, 0, pWidth, pHeight, null);

		gScr.drawImage (mountains, mountainX1-5, pHeight-pHeight*3/4, pWidth+10, pHeight*3/4, null);
		gScr.drawImage (mountains, mountainX2, pHeight-pHeight*3/4, pWidth+10, pHeight*3/4, null);

		gScr.drawImage (rocks, rocksX-10, pHeight-pHeight/2, pWidth+20, pHeight/2, null);
		gScr.drawImage (rocks, rocksX2, pHeight-pHeight/2, pWidth+20, pHeight/2, null);

        gScr.drawImage (grass, grassX1-15, pHeight-57, pWidth+20, 57, null);
        gScr.drawImage (grass, grassX2, pHeight-57, pWidth+20, 57, null);

        Font font = new Font("SansSerif", Font.BOLD, 24);
        gScr.setColor(Color.WHITE);
        gScr.setFont(font);
		gScr.drawString("Score: " + hero.score, 10, 150);

		drawButtons(gScr);			// draw the buttons

		instructions(gScr);

		gScr.setColor(Color.black);

		rat.draw((Graphics2D)gScr);		// draw the ball
		enemyBoundary = rat.getEnemyRectangle();

		hero.draw((Graphics2D)gScr);		// draw the bat
//		nightHowler.draw((Graphics2D)gScr);		// draw the bat

		if (isStopped)				// display game over message
			gameOverMessage(gScr);
	}

	/* This method draws the buttons on the screen. The text on a button
	   is highlighted if the mouse is currently over that button AND if
	   the action of the button can be carried out at the current time.
	*/

	private void drawButtons (Graphics g) {
		Font oldFont, newFont;

		oldFont = g.getFont();		// save current font to restore when finished
	
		newFont = new Font ("TimesRoman", Font.ITALIC + Font.BOLD, 18);
		g.setFont(newFont);		// set this as font for text on buttons

    		g.setColor(Color.black);	// set outline colour of button



		// draw the quit 'button'

		g.setColor(Color.BLACK);
		g.drawOval(quitButtonArea.x, quitButtonArea.y, 
			   quitButtonArea.width, quitButtonArea.height);
		if (isOverQuitButton)
			g.setColor(Color.WHITE);
		else
			g.setColor(Color.RED);

		g.drawString("Quit", quitButtonArea.x+60, quitButtonArea.y+25);
		g.setFont(oldFont);		// reset font

	}

	// displays a message to the screen when the user stops the game

	private void gameOverMessage(Graphics g) {
		
		Font font = new Font("SansSerif", Font.BOLD, 24);
		FontMetrics metrics = this.getFontMetrics(font);

		String msg = "Game Over. Thanks for playing!";

		int x = (pWidth - metrics.stringWidth(msg)) / 2; 
		int y = (pHeight - metrics.getHeight()) / 2;

		g.setColor(Color.BLUE);
		g.setFont(font);
		g.drawString(msg, x, y);

	}

	private void instructions(Graphics g) {

		Font font = new Font("SansSerif", Font.BOLD, 24);

		String msg = "A - Left, D - Right\t|\tESC or Q to Quit";

		int x = 20;
		int y = (pHeight - 24);

		g.setColor(Color.WHITE);
		g.setFont(font);
		g.drawString(msg, x, y);

	}

	/* This method performs some tasks before closing the game.
	   The call to System.exit() should not be necessary; however,
	   it prevents hanging when the game terminates.
	*/

	private void finishOff() { 
    		if (!finishedOff) {
			finishedOff = true;
			restoreScreen();
			System.exit(0);
		}
	}

	/* This method switches off full screen mode. The display
	   mode is also reset if it has been changed.
	*/

	private void restoreScreen() { 
		Window w = device.getFullScreenWindow();
		
		if (w != null)
			w.dispose();
		
		device.setFullScreenWindow(null);
	}

	// This method provides details about the current display mode.

	private void showCurrentMode() {
		DisplayMode dm = device.getDisplayMode();
		System.out.println("Current Display Mode: (" + 
                           dm.getWidth() + "," + dm.getHeight() + "," +
                           dm.getBitDepth() + "," + dm.getRefreshRate() + ")  " );
  	}
	
	public void loadImages() {

		bgImage = loadImage("assets/images/background.png");
		mountains = loadImage("assets/images/mountains.png");
		rocks = loadImage("assets/images/rocks.png");
		grass = loadImage("assets/images/grass.png");

	}

	public Image loadImage (String fileName) {
		return new ImageIcon(fileName).getImage();
	}

	public void loadClips() {

		try {
			playSound = Applet.newAudioClip (
					getClass().getResource("assets/sounds/zelda.wav"));

		}
		catch (Exception e) {
			System.out.println ("Error loading sound file: " + e);
		}

	}

	public void playClip (int index) {

		if (index == 1 && playSound != null)
			playSound.play();

	}

}

