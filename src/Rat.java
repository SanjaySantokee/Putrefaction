import javax.swing.*;
import java.applet.Applet;
import java.applet.AudioClip;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.geom.Rectangle2D;

public class Rat extends Animation {

    private JFrame frame;
    private Hero hero;
    private int check = 0;
    private int scoreMultiplier = 20;
    private long startTime = 0;
    private int pWidth = 0, pHeight = 0;
    private int angle = 0;
    private double accidentTime = System.nanoTime() / 1000000000.0;
    private Dimension dimension;
    private AudioFx sf;
    private int i = 0;
    private boolean isRotated = false;
    private long damageTime;
    private long currentTime;
    private boolean canReset = false;
    private boolean isDamaged = false;
    private int alpha = 255, alphaChange = 5;
    private boolean isVisible = true;
    private AudioClip killratsound;


    public Rat (JFrame f, Hero h, int x, int y, int dx, int dy,
                   int pW, int pH,
                   int xSize, int ySize,
                   String filename){
        super(f, x, y, dx, dy, xSize, ySize, filename);

        this.frame = f;

        dimension = frame.getSize();

        sf = new AudioFx();
        hero = h;
        pWidth = pW;
        pHeight = pH;
        startTime = System.currentTimeMillis();

        setPosition();
        loadClips();


        BufferedImage one = loadImage("assets/images/rat1.png");
        BufferedImage two = loadImage("assets/images/rat2.png");
        BufferedImage three = loadImage("assets/images/rat1.png");
        BufferedImage four = loadImage("assets/images/rat2.png");

        addFrame(one, 500);
        addFrame(two, 500);
        addFrame(three, 500);
        addFrame(four, 500);
    }

    public void update(){

        if (!window.isVisible()) return;

        if (!isDamaged)
            x += dx;

        if (canReset && isDamaged) {
            isDamaged = false;
            canReset = false;
            alpha = 255;
            setPosition();


        }

        boolean hitBat = ratEatsHero();

        if (hitBat && hero.score < 400) {
            hero.damagePlayer();

            try {                    // take a rest if bat hits ball or
                Thread.sleep(1000);        //   ball falls out of play.
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            setPosition();                // re-set position of ball
        }

    }

    public void draw (Graphics2D g2) {
        isOffScreen();

        BufferedImage rat;

        currentTime = System.nanoTime() / 1000000000;
        if (isDamaged && currentTime-damageTime<=3){
            rat = rotateDraw(g2, 2);
            rat = drawGreyImage(g2, rat);
            disappear(g2, rat);
            return;
        }
        else {
            canReset = true;
        }

        if (!isDamaged){
            if (i>=3)
                i = 0;

            rat = getFrame(i).image;
            i++;
            g2.drawImage(rat, x, y, xSize, ySize, null);
        }
    }

    public void damage(){

        isDamaged = true;
        canReset = false;

        long time = (System.currentTimeMillis() - startTime) ;

        if (time < 40000){
            scoreMultiplier = 20;
        }
        if (time > 40000 && time < 100000){
            scoreMultiplier = 15;
        }
        if (time > 100000) {
            scoreMultiplier = 5;
        }

        damageTime = System.nanoTime() / 1000000000;

        hero.score += scoreMultiplier;
        playClip(1);
    }

    public void disappear(Graphics2D g2, BufferedImage copy){
        if (alpha > 0){

            int imWidth = copy.getWidth();
            int imHeight = copy.getHeight();

            int[] pixels = new int[imWidth * imHeight];
            copy.getRGB(0, 0, imWidth, imHeight, pixels, 0, imWidth);

            int red, green, blue, newValue;
            int pixelAlpha;


            for (int i = 0; i < pixels.length; i++) {
                pixelAlpha = (pixels[i] >> 24) & 255;
                red = (pixels[i] >> 16) & 255;
                green = (pixels[i] >> 8) & 255;
                blue = pixels[i] & 255;

                if (pixelAlpha != 0) {
                    newValue = blue | (green << 8) | (red << 16) | (alpha << 24);
                    pixels[i] = newValue;
                }
            }

            copy.setRGB(0, 0, imWidth, imHeight, pixels, 0, imWidth);
            g2.drawImage(copy, x, y, xSize, ySize, null);
        }

        alpha = alpha - alphaChange;
    }

    private BufferedImage rotateDraw(Graphics2D g2, int angle){

        BufferedImage rotatedImg;

        rotatedImg = getFrame(2).image;

        AffineTransform tx = new AffineTransform();
        tx.rotate(Math.toRadians(180), rotatedImg.getWidth() / 2, rotatedImg.getHeight() / 2);

        AffineTransformOp op = new AffineTransformOp(tx,
                AffineTransformOp.TYPE_BILINEAR);
        rotatedImg = op.filter(rotatedImg, null);

        return rotatedImg;
    }

    public int toGray (int pixel) {

        int alpha, red, green, blue, gray;
        int newPixel;

        alpha = (pixel >> 24) & 255;
        red = (pixel >> 16) & 255;
        green = (pixel >> 8) & 255;
        blue = pixel & 255;

        gray = (red + green + blue) / 3;

        red = green = blue = gray;

        newPixel = blue | (green << 8) | (red << 16) | (alpha << 24);
        return newPixel;
    }

    private BufferedImage drawGreyImage(Graphics2D g2, BufferedImage copy){

        int imWidth = copy.getWidth();
        int imHeight = copy.getHeight();

        int [] pixels = new int[imWidth * imHeight];
        copy.getRGB(0, 0, imWidth, imHeight, pixels, 0, imWidth);

        int alpha, red, green, blue, gray;

        for (int i=0; i<pixels.length; i++) {
            pixels[i] = toGray(pixels[i]);
        }

        copy.setRGB(0, 0, imWidth, imHeight, pixels, 0, imWidth);

//        g2.drawImage(copy, x, y, xSize, ySize, null);
        return copy;
    }

    public void setDeviceResolution(int w, int h) {
        pWidth = w;
        pHeight = h;
    }

    public void setPosition() {
        canReset = false;
        setX(pWidth + 10);
        setY(pHeight - 170);
    }

    public Rectangle2D.Double getEnemyRectangle(){
        return getBoundingRectangle();
    }

    public boolean ratEatsHero() {

        Rectangle2D.Double rectBall = getBoundingRectangle();
        Rectangle2D.Double rectBat = hero.getBoundingRectangle();

        if (rectBall.intersects(rectBat))
            return true;
        else
            return false;
    }

    public void isOffScreen(){


        if (x < 0){
            setPosition();
        }

    }

    public void loadClips() {

        try {
            killratsound = Applet.newAudioClip (
                    getClass().getResource("assets/sounds/kill.wav"));
        }
        catch (Exception e) {
            System.out.println ("Error loading sound file: " + e);
        }

    }

    public void playClip(int index) {

        if (index == 1 && killratsound != null)
            killratsound.play();
    }
}
