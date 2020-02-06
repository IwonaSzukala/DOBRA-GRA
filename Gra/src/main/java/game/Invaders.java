package game;

import actors.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class Invaders extends Stage implements KeyListener {

    private static final long serialVersionUID = 1L;

    private Player player;
    private InputHandler keyPressedHandler;
    private InputHandler keyReleasedHandler;

    public long usedTime; //time taken per game step
    public BufferStrategy strategy; //double buffering strategy

    private BufferedImage background, backgroundTitle; //background cache
    private int backgroundY; //background cache position

    private Invaders() {
        //int the UI
        setBounds(0, 0, Stage.WIDTH, Stage.HEIGHT);
        setBackground(Color.BLACK);

        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(Stage.WIDTH, Stage.HEIGHT));
        panel.setLayout(null);

        panel.add(this);

        JFrame frame = new JFrame("Invaders");
        frame.add(panel);
        frame.setBounds(0, 0, Stage.WIDTH, Stage.HEIGHT);
        frame.setResizable(false);
        frame.setVisible(true);

        //cleanup rescoures on exit
        frame.addWindowFocusListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                ResourceLoader.getInstance().cleanup();
                System.exit(0);
            }
        });

        addKeyListener(this);

        //create a double buffer
        createBufferStrategy(2);
        strategy = getBufferStrategy();
        requestFocus();
        initWorld();

        keyPressedHandler = new InputHandler(this, player);
        keyPressedHandler.action = InputHandler.Action.PRESS;
        keyReleasedHandler = new InputHandler(this, player);
        keyReleasedHandler.action = InputHandler.Action.RELEASE;
    }

    /*
    add a grid of invaders based on the screen size
     */

    public void addInvaders() {
        Invader invader = new Invader(this);
        //padding between units/rows
        int xPad = invader.getWidth() + 15;
        int yPad = invader.getHeight() + 20;
        //number of units per row
        int unitsPerRow = Stage.WIDTH / (xPad) - 1;
        int rows = (Stage.HEIGHT / yPad) - 3; //number of invader rows

        //create and add invaders for each row
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < unitsPerRow - 1; j++) {
                Invader inv = new Invader(this);
                inv.setX((j + 1) * xPad);
                inv.setY((i + 1) * yPad);
                inv.setVx(10);
                //set movement boundaries for each invader
                inv.setLeftWall((j + 1) * xPad - 20);
                inv.setRightWall((j + 1) * xPad + 20);
                actors.add(inv);
            }
        }
    }

    public void initWorld() {
        actors = new ArrayList<Actor>();
        gameOver = false;
        gameWon = false;
        //add a player
        player = new Player(this);
        player.setX(Stage.WIDTH / 2 - player.getWidth() / 2);
        player.setY(Stage.HEIGHT - 50);
        player.setVx(10);

        //load cached background
        backgroundTitle = ResourceLoader.getInstance().getSprite("space.gif");
        background = ResourceLoader.createCompatible(
                WIDTH, HEIGHT + backgroundTitle.getHeight(),
                Transparency.OPAQUE);
        Graphics2D g = (Graphics2D) background.getGraphics();
        g.setPaint(new TexturePaint(backgroundTitle, new Rectangle(0, 0, backgroundTitle.getWidth(), backgroundTitle.getHeight())));
        g.fillRect(0, 0, background.getWidth(), background.getHeight());
        backgroundY = backgroundTitle.getHeight();

        addInvaders();
    }

    public void paintWorld() {

        Graphics g = strategy.getDrawGraphics();

        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());

        g.drawImage(background, 0, 0, Stage.WIDTH, Stage.HEIGHT, 0, backgroundY, Stage.WIDTH, backgroundY + Stage.HEIGHT, this);

        for (int i = 0; i < actors.size(); i++) {
            Actor actor = actors.get(i);
            actor.paint(g);
        }

        player.paint(g);
        paintScore(g);
        paintFPS(g);

        strategy.show();
    }

    public void paintGameOver() {
        Graphics g = strategy.getDrawGraphics();
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());

        paintScore(g);

        g.setFont(new Font("Arial", Font.BOLD, 50));
        g.setColor(Color.RED);
        int xPos = getWidth() / 2 - 155;
        g.drawString("GAME OVER", (xPos < 0 ? 0 : xPos), getHeight() / 2);

        xPos += 30;
        g.setFont(new Font("Arial", Font.BOLD, 30));
        g.drawString("ENTER: try again", (xPos < 0 ? 0 : xPos), getHeight() / 2 + 50);

        strategy.show();
    }

    public void paintGameWon() {
        Graphics g = strategy.getDrawGraphics();
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());

        paintScore(g);


        g.setFont(new Font("Arial", Font.BOLD, 50));
        g.setColor(Color.RED);
        int xPos = getWidth() / 2 - 145;
        g.drawString("GAME WON", (xPos < 0 ? 0 : xPos), getHeight() / 2);

        xPos += 20;
        g.setFont(new Font("Arial", Font.BOLD, 30));
        g.drawString("ENTER: try again", (xPos < 0 ? 0 : xPos),getHeight() / 2 + 50);

        strategy.show();
    }

    public void paintFPS(Graphics g) {
        g.setColor(Color.RED);
        if (usedTime > 0)
            g.drawString(String.valueOf(1000 / usedTime) + " fps", 0, Stage.HEIGHT - 50);
        else
            g.drawString("--- fps", 0, Stage.HEIGHT - 50);
    }

    public void paintScore(Graphics g) {
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.setColor(Color.GREEN);
        g.drawString("Score: ", 20, 20);
        g.setColor(Color.RED);
        g.drawString("" + player.getScore(), 100, 20);
    }

    public void paint(Graphics g) {
    }

    public void updateWorld() {

        int i = 0;
        int numInvaders = 0;
        while (i < actors.size()) {
            Actor actor = actors.get(i);
            if (actor instanceof Shot)
                checkCollision(actor);

            if (actor.isMarkedForRemoval()) {
                player.updateScore(actor.getPointValue());
            } else {
                if (actor instanceof Invader)
                    numInvaders++;
                actor.act();
                i++;
            }
        }
        if (numInvaders == 0)
            super.gameWon = true;

        checkCollision(player);
        player.act();
    }

    private void checkCollision(Actor actor) {

        Rectangle actorBounds = actor.getBounds();
        for (int i = 0; i <actors.size();
        i++){
            Actor otherActor = actors.get(i);
            if (null == otherActor || actor.equals(otherActor)) continue;
            if (actorBounds.intersects(otherActor.getBounds())) {
                actor.collision(otherActor);
                otherActor.collision(actor);
            }

        }
    }

    public void loopSound(final String name) {
        new Thread(new Runnable() {
            public void run() {
                ResourceLoader.getInstance().getSound(name).loop();
            }
        }).start();

    }

    public void game() {
        loopSound("music.wav");
        usedTime = 0;
        while(isVisible()) {
            long startTime = System.currentTimeMillis();

            backgroundY--;
            if (backgroundY < 0)
                backgroundY = backgroundTitle.getHeight();

            if (super.gameOver) {
                paintGameOver();
                break;
            }
            else if (super.gameWon) {
                paintGameWon();
                break;
            }

            int random = (int)(Math.random()*1000);
            if (random == 700) {
                Actor ufo = new Ufo(this);
                ufo.setX(0);
                ufo.setY(20);
                ufo.setVx(1);
                actors.add(ufo);
            }

            updateWorld();
            paintWorld();

            usedTime = System.currentTimeMillis() - startTime;

            //calculate sleep time
            if (usedTime == 0) usedTime = 1;
            int timeDiff = (int) ((100/usedTime) - DESIRED_FPS);
            if (timeDiff > 0) {
                try {
                    Thread.sleep(timeDiff/100);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void keyPressed(KeyEvent e) {
        keyPressedHandler.handleInput (e);
    }

    public void keyReleased(KeyEvent e) {
        keyReleasedHandler.handleInput(e);
    }

    public void keyTyped(KeyEvent e) {

    }

    public static void main(String[] args) {
        Invaders inv = new Invaders();
        inv.game();
    }


}



