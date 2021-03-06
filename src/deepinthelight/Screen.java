package deepinthelight;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.newdawn.slick.geom.Shape;
import org.newdawn.slick.SlickException;

public class Screen {

    private static HashMap<String, Screen> ScreenMap = null;
    private static Random generator = null;
    private static final int maxObstacleSize = 10;
    private static final int objectTypeNumber = 8;

    private int obstacleSize = 0;
    private boolean populated;
    private ArrayList<Element> elements;
    private boolean elementChanged;

    public enum Zone {
        UP, DOWN, LEFT, RIGHT, UPLEFT, UPRIGHT, DOWNLEFT, DOWNRIGHT
    }

    private float x;
    private float y;

    private Screen(float x, float y) {

        //System.out.println("New screen : " + x + ", " + y);
        this.x = x;
        this.y = y;
        populated = false;
        elementChanged = false;
        elements = new ArrayList<Element>(100);
        ScreenMap.put(this.serialize(), this);
    }

    public ArrayList<Element> getAllElements(int neighborhood, Zone propagationDirection) {
        ArrayList<Element> allElements = new ArrayList<Element>();
        allElements.addAll(elements);
        if (neighborhood > 0) {
            switch (propagationDirection) {
            case UP :
                allElements.addAll(getNextScreen(Zone.UP).getAllElements(neighborhood-1));
                allElements.addAll(getNextScreen(Zone.UPLEFT).getAllElements(neighborhood-1));
                //allElements.addAll(getNextScreen(Zone.UPRIGHT).getAllElements(neighborhood-1));
                break;
            case DOWN:
                //allElements.addAll(getNextScreen(Zone.DOWN).getAllElements(neighborhood-1));
                //allElements.addAll(getNextScreen(Zone.DOWNLEFT).getAllElements(neighborhood-1));
                //allElements.addAll(getNextScreen(Zone.DOWNRIGHT).getAllElements(neighborhood-1));
                break;
            case LEFT:
                allElements.addAll(getNextScreen(Zone.LEFT).getAllElements(neighborhood-1));
                //allElements.addAll(getNextScreen(Zone.DOWNLEFT).getAllElements(neighborhood-1));
                allElements.addAll(getNextScreen(Zone.UPLEFT).getAllElements(neighborhood-1));
                break;
            case RIGHT:
                //allElements.addAll(getNextScreen(Zone.RIGHT).getAllElements(neighborhood-1));
                //allElements.addAll(getNextScreen(Zone.UPRIGHT).getAllElements(neighborhood-1));
                //allElements.addAll(getNextScreen(Zone.DOWNRIGHT).getAllElements(neighborhood-1));
                break;
            case UPRIGHT:
                break;
            default :
                allElements.addAll(getNextScreen(propagationDirection).getAllElements(neighborhood-1));
            }
        }

        return allElements;
    }

    public ArrayList<Element> getAllElements(int neighborhood) {
        ArrayList<Element> allElements = new ArrayList<Element>();
        allElements.addAll(elements);

        if (neighborhood > 0) {
            for (Zone where : Zone.values()) {
                allElements.addAll(getNextScreen(where).getAllElements(neighborhood-1, where));
            }
        }

        return allElements;
    }

    public ArrayList<Element> getElements() {
        elementChanged = false;
        return elements;
    }

    public static Screen init(float screenX, float screenY) {
        //System.out.println("Init screen");
//        if (ScreenMap != null) {
//            return null;
//        }

        generator = new Random();
        ScreenMap = new HashMap<String, Screen>(100);
        return new Screen(screenY, screenX);
    }

    public Screen getNextScreen(Gunther gunther) {
        float top = y;
        float bottom = y + Main.height;
        float left = x;
        float right = x + Main.width;
        Shape box = gunther.getBox();
        Screen nextScreen = null;
        if (box.getCenterY() < top) {
            if (box.getCenterX() < left) {
                nextScreen = getNextScreen(Zone.UPLEFT);
            } else if (box.getCenterX() >= right) {
                nextScreen = getNextScreen(Zone.UPRIGHT);
            }

            nextScreen = getNextScreen(Zone.UP);
        } else if (box.getCenterY() >= bottom) {
            if (box.getCenterX() < left) {
                nextScreen = getNextScreen(Zone.DOWNLEFT);
            } else if (box.getCenterX() >= right) {
                nextScreen = getNextScreen(Zone.DOWNRIGHT);
            }

            nextScreen = getNextScreen(Zone.DOWN);
        } else if (box.getCenterX() < left) {
            nextScreen = getNextScreen(Zone.LEFT);
        } else if (box.getCenterX() >= right) {
            nextScreen = getNextScreen(Zone.RIGHT);
        }

        return nextScreen;
    }

    public Screen getNextScreen(Zone where) {
        float nextx = x;
        float nexty = y;
        switch(where) {
        case UPLEFT:
            nextx -= Main.width;
        case UP :
            nexty -= Main.height;
            break;
        case DOWNRIGHT :
            nextx += Main.width;
        case DOWN :
            nexty += Main.height;
            break;
        case DOWNLEFT:
            nexty += Main.height;
        case LEFT :
            nextx -= Main.width;
            break;
        case UPRIGHT :
            nexty -= Main.height;
        case RIGHT :
            nextx += Main.width;
            break;
        }

        return getScreen(nextx, nexty);
    }

    private Screen getScreen(float x, float y) {
        Screen nextScreen = ScreenMap.get(Screen.serialize(x, y));
        if (nextScreen == null) {
            nextScreen = new Screen(x, y);
        }

        return nextScreen;
    }

    public boolean isInScreen(Gunther gunther) {
        float top = y;
        float bottom = y + Main.height;
        float left = x;
        float right = x + Main.width;
        Shape box = gunther.getBox();
        
        if (box.getCenterY() >= top && box.getCenterY() < bottom &&
            box.getCenterX() >= left && box.getCenterX() < right) {
            //System.out.println("Gunther is in screen " + x + ", " + y);
            return true;
        }

        //System.out.println("Gunther is NOT in screen " + x + ", " + y);
        return false;
    }

    public void populateNeighbors(int level) {
        System.out.println("Populating neighbors");
        for (Zone where : Zone.values()) {
            this.getNextScreen(where).populate(level);
        }
    }

    private void populate(int neighborhood) {
        this.populate();
        if (neighborhood != 0) {
            populateNeighbors(neighborhood -1);
        }
    }

    public void populate() {
        if (populated || (x==0 && y==0)) {
            return;
        }

        System.out.println("Populating " + x + ", " + y);
        final int maxIter = 40;
        int i = 0;
        while (obstacleSize < maxObstacleSize && i < maxIter) {
            Obstacle obs = createObstacle();
            if (obs != null) {
                elements.add(obs);
            }

            Malus malus = createMalus();
            if (malus != null) {
                elements.add(malus);
            }

            BonusFish bf = createBF();
            if (bf != null) {
                elements.add(bf);
            }
            i++;
        }

        elementChanged = true;
        populated = true;
    }

    public Obstacle createObstacle() {
        int type = generator.nextInt(8);
        float centerX = x + generator.nextInt(Main.width);
        float centerY = y + generator.nextInt(Main.height);
        Obstacle obs = null;
        try {
            obs = new Obstacle(type, centerX, centerY);
        } catch (SlickException ex) {
            ex.printStackTrace();
            return null;
        }

        for (Element element : getAllElements(2)) {
            if (obs.getBox().intersects(element.getBox()) || obs.getBox().contains(element.getBox())) {
                return null;
            }
        }

        if(obs.getBox().intersects(GamePlay.getGamePlay().gunther.getBox()) || obs.getBox().contains(GamePlay.getGamePlay().gunther.getBox())) {
            return null;
        }

        obstacleSize += obs.getSize();
        return obs;
    }

    public Malus createMalus() {
        float centerX = x + generator.nextInt(Main.width);
        float centerY = y + generator.nextInt(Main.height);
        Malus malus = null;
        try {
            malus = new Malus(centerX, centerY, this);
        } catch (SlickException ex) {
            ex.printStackTrace();
            return null;
        }

        for (Element element : getAllElements(2)) {
            if (element.getScreen() != this && element.getSize() == 1) {
                continue;
            }
            if (malus.getBox().intersects(element.getBox()) || malus.getBox().contains(element.getBox())) {
                return null;
            }
        }

        obstacleSize += malus.getSize();
        return malus;
    }

    public BonusFish createBF() {
        float centerX = x + generator.nextInt(Main.width);
        float centerY = y + generator.nextInt(Main.height);
        BonusFish bf = null;
        try {
            bf = new BonusFish(centerX, centerY, this);
        } catch (SlickException ex) {
            ex.printStackTrace();
            return null;
        }

        for (Element element : getAllElements(2)) {
            if (element.getScreen() != this && element.getSize() == 1) {
                continue;
            }
            if (bf.getBox().intersects(element.getBox()) || bf.getBox().contains(element.getBox())) {
                return null;
            }
        }

        obstacleSize += bf.getSize();
        return bf;
    }

    public void deleteElement(Element el) {
        elements.remove(el);
        elementChanged = true;
    }

    public boolean elementChanged() {
        return elementChanged;
    }

    private String serialize() {
        return Screen.serialize(x, y);
    }

    private static String serialize(float x, float y) {
        return new String("X" + Math.round(x) + "Y" + Math.round(y));
    }

    public float getTopBoundary() {
        return y;
    }

    public float getBottomBoundary() {
        return y + Main.height;
    }

    public float getLeftBoundary() {
        return x;
    }

    public float getRightBoundary() {
        return x + Main.width;
    }

}
