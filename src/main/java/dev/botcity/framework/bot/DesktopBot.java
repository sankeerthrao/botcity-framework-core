package dev.botcity.framework.bot;
import static org.marvinproject.plugins.collection.MarvinPluginCollection.crop;
import static org.marvinproject.plugins.collection.MarvinPluginCollection.thresholding;

import java.awt.Desktop;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.marvinproject.framework.image.MarvinImage;
import org.marvinproject.framework.image.MarvinSegment;
import org.marvinproject.framework.io.MarvinImageIO;
import org.marvinproject.framework.plugin.MarvinImagePlugin;
import org.marvinproject.plugins.image.transform.flip.Flip;

public class DesktopBot {

	private Robot 						robot;
	private Integer 					x,
										y;
	
	private MarvinImage 				screen,
										visualElem;
	
	private UIElement					lastElement = new UIElement();
	
	private MarvinImagePlugin 			flip;
	
	private boolean 					debug=false;
	
	private int 						defaultSleepAfterAction=300;
	
	private ClassLoader					resourceClassLoader;
	
	private Map<String, MarvinImage>	mapImages;
	
	public DesktopBot() {
		try {
			robot = new Robot();
			mapImages = new HashMap<String, MarvinImage>();
		} catch(Exception e) {
			e.printStackTrace();
		}
		screen = new MarvinImage(1,1);
		
		//flip = MarvinPluginLoader.loadImagePlugin("org.marvinproject.image.transform.flip");
		flip = new Flip();
		flip.setAttribute("flip", "vertical");
	}
	
	public void enableDebug(){
		this.debug = true;
	}
	
	public void setResourceClassLoader(ClassLoader classloader) {
		this.resourceClassLoader = classloader;
	}
	
	public void addImage(String label, String path) {
		File f = new File(path);
		
		// file outside jar?
		if(f.exists())
			mapImages.put(label, MarvinImageIO.loadImage(path));
		else {
			if(this.resourceClassLoader != null) {
				ImageIcon img = new ImageIcon(this.resourceClassLoader.getResource(path));
				mapImages.put("label", new MarvinImage(toBufferedImage(img.getImage())));
			}
		}
	}
	
	public void addImage(String label, MarvinImage image) {
		mapImages.put(label, image);
	}
	
	private static BufferedImage toBufferedImage(Image img)	{
	    if (img instanceof BufferedImage)
	    {
	        return (BufferedImage) img;
	    }
	    // Create a buffered image with transparency
	    BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
	    // Draw the image on to the buffered image
	    Graphics2D bGr = bimage.createGraphics();
	    bGr.drawImage(img, 0, 0, null);
	    bGr.dispose();
	    // Return the buffered image
	    return bimage;
	}
	
	private MarvinImage getImageFromMap(String label) {
		return mapImages.get(label);
	}
	
	public Robot getRobot() {
		return this.robot;
	}
	
	public UIElement getLastElement() {
		return this.lastElement;
	}
	
	public void exec(String command) throws IOException {
		Runtime.getRuntime().exec(command);
	}
	
	public void browse(String uri) throws IOException {
		try {
			Desktop.getDesktop().browse(new URI(uri));
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public boolean clickOn(MarvinImage visualElem) {
		screenshot();
		Point p = getElementCoordsCentered(visualElem, 0.95);
		if(p != null) {
			mouseMove(p.x, p.y);
			robot.mousePress(InputEvent.BUTTON1_MASK);
			robot.mouseRelease(InputEvent.BUTTON1_MASK);
			
			this.x = p.x;
			this.y = p.y;
			return true;
		}
		return false;
	}
	
	public Integer getLastX() {
		return this.x;
	}
	
	public Integer getLastY() {
		return this.y;
	}
	
	
	static int id=0;
//	public boolean findUntil(String elementImage, int maxWaitingTime) {
//		visualElem = MarvinImageIO.loadImage(elementImage);
//		return findUntil(visualElem, maxWaitingTime);
//	}
	
	public boolean findText(String elementId,int maxWaitingTime) {
		return findText(elementId, getImageFromMap(elementId), null, maxWaitingTime);
	}
	
	public boolean findText(String elementId, MarvinImage visualElem, int maxWaitingTime) {
		return findText(elementId, visualElem, null, maxWaitingTime);
	}
	
	public boolean findText(String elementId, Integer threshold, int maxWaitingTime) {
		return findText(elementId, getImageFromMap(elementId), threshold, maxWaitingTime);
	}
	
	public boolean findText(String elementId, MarvinImage visualElem, Integer threshold, int maxWaitingTime) {
		if(threshold == null) {
			return findUntil(elementId, visualElem, threshold, 0.9, maxWaitingTime);
		} else {
			return findUntil(elementId, visualElem, threshold, 0.85, maxWaitingTime);
		}
	}
	
	public boolean find(String elementId, Double elementMatching, int maxWaitingTime) {
		return find(elementId, getImageFromMap(elementId), elementMatching, maxWaitingTime);
	}
	
	public boolean find(String elementId, MarvinImage visualElem, Double elementMatching, int maxWaitingTime) {
		return findUntil(elementId, visualElem, null, elementMatching, maxWaitingTime);
	}
	
	
	public boolean findUntil(String elementId, Integer threshold, Double elementMatching, int maxWaitingTime) {
		return findUntil(elementId, getImageFromMap(elementId), threshold, elementMatching, maxWaitingTime);
	}
	
	public boolean findUntil(String elementId, MarvinImage visualElem, Integer threshold, Double elementMatching, int maxWaitingTime) {
		return findUntil(elementId, visualElem, null, null, null, null, threshold, elementMatching, maxWaitingTime);
	}
	
	public boolean findRelative
	(
		String elementId,
		MarvinImage visualElem,
		UIElement anchor,
		int xDiff,
		int yDiff,
		int searchWindowWidth,
		int searchWindowHeight,
		Integer threshold,
		Double elementMatching,
		int maxWaitingTim
	) {
		return findUntil(elementId, visualElem, anchor.getX()+xDiff, anchor.getY()+yDiff, searchWindowWidth, searchWindowHeight, threshold, elementMatching, maxWaitingTim);
	}
	
	public boolean findUntil
	(
		String elementId, 
		MarvinImage visualElem,
		Integer startX,
		Integer startY,
		Integer searchWindowWidth,
		Integer searchWindowHeight,
		Integer threshold, 
		Double elementMatching, 
		int maxWaitingTime
	) {
		long startTime = System.currentTimeMillis();
		while(true) {
			
			if(System.currentTimeMillis() - startTime > maxWaitingTime) {
//				System.out.println("false");
				return false;
			}
			
			sleep(100);
			screenshot();
			
//			System.out.println("Threshold:"+threshold);
			
			Point p=null;
			
			startX = (startX != null ? startX : 0);
			startY = (startY != null ? startY : 0);
			searchWindowWidth = (searchWindowWidth != null ? searchWindowWidth : screen.getWidth());
			searchWindowHeight = (searchWindowHeight != null ? searchWindowHeight : screen.getHeight());
			
			if(threshold != null) {
				
				
				
				MarvinImage screenCopy = screen.clone();
				thresholding(screenCopy, threshold);
				
				MarvinImage visualElemCopy = visualElem.clone();
				thresholding(visualElemCopy, threshold);
				
				if(debug) {
					MarvinImageIO.saveImage(screen, "./debug/screen.png");
					MarvinImageIO.saveImage(visualElem, "./debug/visualElem.png");
					MarvinImageIO.saveImage(screenCopy, "./debug/screenCopy.png");
					MarvinImageIO.saveImage(visualElemCopy, "./debug/visualElemCopy.png");
				}
				
				p = getElementCoords(visualElemCopy, screenCopy, startX, startY, searchWindowWidth, searchWindowHeight, elementMatching);
			} else {
				p = getElementCoords(visualElem, startX, startY, searchWindowWidth, searchWindowHeight, elementMatching);
				
				if(debug) {
					MarvinImageIO.saveImage(screen, "./debug/screenCopy.png");
					MarvinImageIO.saveImage(visualElem, "./debug/visualElemCopy.png");
				}
			}
			
			if(p != null) {
				this.visualElem = visualElem;
				
				if(debug)
					System.out.println("found:"+p.x+","+p.y+": "+elementId);
				
				this.x = p.x;
				this.y = p.y;
				
				lastElement.setX(p.x);
				lastElement.setY(p.y);
				lastElement.setImage(this.visualElem);
				
				return true;
			} else {
				//System.out.println("not found: "+elementImage);
				//MarvinImageIO.saveImage(screen, "./res/screenshot_"+(id++)+".png");
			}
		}
	}
	
	public Point getCoordinates(String elementImage, int maxWaitingTime) {
		long startTime = System.currentTimeMillis();
		while(true) {
			
			if(System.currentTimeMillis() - startTime > maxWaitingTime) {
//				System.out.println("false");
				return null;
			}
			
			sleep(300);
			screenshot();
			visualElem = MarvinImageIO.loadImage(elementImage);
			Point p = getElementCoords(visualElem, 0.95);
			
			if(p != null) {
				
				if(debug)
					System.out.println("found:"+p.x+","+p.y+": "+elementImage);
				
				return p;
			} else {
				//System.out.println("not found: "+elementImage);
				//MarvinImageIO.saveImage(screen, "./res/screenshot_"+(id++)+".png");
			}
		}
	}
	
	public boolean findLastUntil(String elementId, MarvinImage visualElem, int maxWaitingTime) {
		return findLastUntil(elementId, visualElem, null, maxWaitingTime);
	}
	
	public boolean findLastUntil(String elementId, MarvinImage visualElem, Integer threshold, int maxWaitingTime) {
		long startTime = System.currentTimeMillis();
		while(true) {
			
			if(System.currentTimeMillis() - startTime > maxWaitingTime) {
//				System.out.println("false");
				return false;
			}
			
			sleep(300);
			screenshot();
			
			MarvinImage screenCopy = screen.clone();
			flip.process(screen, screenCopy);
			
			MarvinImage visualElemCopy = visualElem.clone();
			flip.process(visualElem, visualElemCopy);
			
			Point p;
			
			if(threshold != null) {
				
				thresholding(screenCopy, threshold);
				thresholding(visualElemCopy, threshold);
				
				if(debug) {
					MarvinImageIO.saveImage(screenCopy, "./debug/screenCopy.png");
					MarvinImageIO.saveImage(visualElemCopy, "./debug/visualElemCopy.png");
				}
				
				p = getElementCoords(visualElemCopy, screenCopy, 0.95);
			} else {
				p = getElementCoords(visualElemCopy, screenCopy, 0.95);
			}
			
			if(p != null) {
				this.visualElem = visualElem;
				
				if(debug)
					System.out.println("found:"+p.x+","+p.y+": "+elementId);
				
				this.x = p.x;
				this.y = screen.getHeight()-(p.y+visualElem.getHeight());
				return true;
			} else {
				//System.out.println("not found: "+elementImage);
				//MarvinImageIO.saveImage(screen, "./res/screenshot_"+(id++)+".png");
			}
		}
	}
	
	private void mouseMove(int px, int py) {
		Point p;
		do{
			p = MouseInfo.getPointerInfo().getLocation();
			robot.mouseMove(px,  py);
		}while(p.x != px || p.y != py);
		
		p = MouseInfo.getPointerInfo().getLocation();
		
		this.x = px;
		this.y = py;
	}
	
	public void clickAt(int px, int py) {
		this.x = px;
		this.y = py;
		moveAndclick();
		
	}
	
	public void click() {
		clickRelative(visualElem.getWidth()/2, visualElem.getHeight()/2);
		sleep(defaultSleepAfterAction);
	}
	
	public void doubleclick() {
		doubleClickRelative(visualElem.getWidth()/2, visualElem.getHeight()/2);
		sleep(defaultSleepAfterAction);
	}
	
	public void clickRelative(int x, int y) {
		this.x += x;
		this.y += y;
		moveAndclick();
		sleep(defaultSleepAfterAction);
	}
	
	public void doubleClickRelative(int x, int y) {
		this.x += x;
		this.y += y;
		moveAndclick();
		sleep(300);
		moveAndclick();
		sleep(defaultSleepAfterAction);
	}
	
	public void tripleClickRelative(int x, int y) {
		this.x += x;
		this.y += y;
		moveAndclick();
		sleep(100);
		moveAndclick();
		sleep(100);
		moveAndclick();
		sleep(defaultSleepAfterAction);
	}
	
	public void scrollDown(int y) {
		robot.mouseWheel(y);
	}
	
	public void scrollUp(int y) {
		robot.mouseWheel(-y);
	}
	
	public void move() {
		moveRelative(visualElem.getWidth()/2, visualElem.getHeight()/2);
	}
	
	public void moveTo(int x, int y) {
		mouseMove(x, y);
		this.x = x;
		this.y = y;
	}
	
	public void moveRelative(int x, int y) {
		mouseMove(this.x+x, this.y+y);
	}
	
	public void moveRandom(int rangeX, int rangeY) {
		int x = (int)Math.round((Math.random()*rangeX));
		int y = (int)Math.round((Math.random()*rangeY));
		moveRelative(x, y);
	}
	
	public void type(String text) {
		for(int i=0; i<text.length(); i++) {
			typeKey(text.charAt(i));
		}
		sleep(defaultSleepAfterAction);
	}
	
	public void typeWaitAfterChars(String text, int waitAfterChars) {
		for(int i=0; i<text.length(); i++) {
			typeKey(text.charAt(i));
			sleep(waitAfterChars);
		}
		sleep(defaultSleepAfterAction);
	}
	
	public void typeWaitAfterChars(String text, int waitAfterChars, int waitAfter) {
		typeWaitAfterChars(text, waitAfterChars);
		sleep(waitAfter);
	}
	
	public void type(String text, int waitAfterChars, int waitAfter) {
		typeWaitAfterChars(text, waitAfterChars);
		sleep(waitAfter);
	}
	
	public void type(String text, int waitAfter) {
		type(text);
		sleep(waitAfter);
	}
	
	public void paste(String text) {
		paste(text, 0);
	}
	
	public void paste(String text, int waitAfter) {
		try {
			StringSelection selection = new StringSelection(text);
			Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
			clip.setContents(selection, selection);
			sleep(500);
			controlV();
			sleep(waitAfter);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void copyToClipboard(String text, int waitAfter) {
		copyToClipboard(text);
		sleep(waitAfter);
	}
	
	public void copyToClipboard(String text) {
		StringSelection stringSelection = new StringSelection(text);
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(stringSelection, null);
	}
	
	private void moveAndclick() {
		mouseMove(this.x, this.y);
		
		robot.mousePress(InputEvent.BUTTON1_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_MASK);
		sleep(defaultSleepAfterAction);
	}
	
	private void click(int waitAfter) {
		moveAndclick();
		sleep(waitAfter);
	}
	
	public void tab() {
		robot.keyPress(KeyEvent.VK_TAB);
		robot.keyRelease(KeyEvent.VK_TAB);
		sleep(defaultSleepAfterAction);
	}
	
	public void tab(int waitAfter) {
		tab();
		sleep(waitAfter);
	}
	
	public void keyRight() {
		robot.keyPress(KeyEvent.VK_RIGHT);
		robot.keyRelease(KeyEvent.VK_RIGHT);
		sleep(defaultSleepAfterAction);
	}
	
	public void keyRight(int waitAfter) {
		keyRight();
		sleep(waitAfter);
	}
	
	public void enter() {
		robot.keyPress(KeyEvent.VK_ENTER);
		robot.keyRelease(KeyEvent.VK_ENTER);
		sleep(defaultSleepAfterAction);
	}
	
	public void keyEnter(int waitAfter) {
		enter();
		sleep(waitAfter);
	}
	
	public void keyEnd() {
		robot.keyPress(KeyEvent.VK_END);
		robot.keyRelease(KeyEvent.VK_END);
		sleep(defaultSleepAfterAction);
	}
	
	public void keyEnd(int waitAfter) {
		keyEnd();
		sleep(waitAfter);
	}
	
	public void keyEsc() {
		robot.keyPress(KeyEvent.VK_ESCAPE);
		robot.keyRelease(KeyEvent.VK_ESCAPE);
		sleep(defaultSleepAfterAction);
	}
	
	public void keyEsc(int waitAfter) {
		keyEsc();
		sleep(waitAfter);
	}
	
	public void keyF1() {					robot.keyPress(KeyEvent.VK_F1);		robot.keyRelease(KeyEvent.VK_F1);	sleep(defaultSleepAfterAction);}
	public void keyF2() {					robot.keyPress(KeyEvent.VK_F2);		robot.keyRelease(KeyEvent.VK_F2);	sleep(defaultSleepAfterAction);}
	public void keyF3() {					robot.keyPress(KeyEvent.VK_F3);		robot.keyRelease(KeyEvent.VK_F3);	sleep(defaultSleepAfterAction);}
	public void keyF4() {					robot.keyPress(KeyEvent.VK_F4);		robot.keyRelease(KeyEvent.VK_F4);	sleep(defaultSleepAfterAction);}
	public void keyF5() {					robot.keyPress(KeyEvent.VK_F5);		robot.keyRelease(KeyEvent.VK_F5);	sleep(defaultSleepAfterAction);}
	public void keyF6() {					robot.keyPress(KeyEvent.VK_F6);		robot.keyRelease(KeyEvent.VK_F6);	sleep(defaultSleepAfterAction);}
	public void keyF7() {					robot.keyPress(KeyEvent.VK_F7);		robot.keyRelease(KeyEvent.VK_F7);	sleep(defaultSleepAfterAction);}
	public void keyF8() {					robot.keyPress(KeyEvent.VK_F8);		robot.keyRelease(KeyEvent.VK_F8);	sleep(defaultSleepAfterAction);}
	public void keyF9() {					robot.keyPress(KeyEvent.VK_F9);		robot.keyRelease(KeyEvent.VK_F9);	sleep(defaultSleepAfterAction);}
	public void keyF10() {					robot.keyPress(KeyEvent.VK_F10);	robot.keyRelease(KeyEvent.VK_F10);	sleep(defaultSleepAfterAction);}
	public void keyF11() {					robot.keyPress(KeyEvent.VK_F11);	robot.keyRelease(KeyEvent.VK_F11);	sleep(defaultSleepAfterAction);}
	public void keyF12() {					robot.keyPress(KeyEvent.VK_F12);	robot.keyRelease(KeyEvent.VK_F12);	sleep(defaultSleepAfterAction);}
	
	public void keyF1(int waitAfter) 	{	keyF1();	sleep(waitAfter);	}
	public void keyF2(int waitAfter) 	{	keyF2();	sleep(waitAfter);	}
	public void keyF3(int waitAfter) 	{	keyF3();	sleep(waitAfter);	}
	public void keyF4(int waitAfter) 	{	keyF4();	sleep(waitAfter);	}
	public void keyF5(int waitAfter) 	{	keyF5();	sleep(waitAfter);	}
	public void keyF6(int waitAfter) 	{	keyF6();	sleep(waitAfter);	}
	public void keyF7(int waitAfter) 	{	keyF7();	sleep(waitAfter);	}
	public void keyF8(int waitAfter) 	{	keyF8();	sleep(waitAfter);	}
	public void keyF9(int waitAfter) 	{	keyF9();	sleep(waitAfter);	}
	public void keyF10(int waitAfter) 	{	keyF10();	sleep(waitAfter);	}
	public void keyF11(int waitAfter) 	{	keyF11();	sleep(waitAfter);	}
	public void keyF12(int waitAfter) 	{	keyF12();	sleep(waitAfter);	}
	
	public void holdShift() {
		robot.keyPress(KeyEvent.VK_SHIFT);
	}
	
	public void holdShift(int waitAfter) {
		robot.keyPress(KeyEvent.VK_SHIFT);
		sleep(waitAfter);
	}
	
	public void releaseShift() {
		robot.keyRelease(KeyEvent.VK_SHIFT);
	}
	
	public void maximizeWindow() {
		altSpace();
		sleep(1000);
		robot.keyPress(KeyEvent.VK_X);
		robot.keyRelease(KeyEvent.VK_X);
	}
	
	public void typeKeys(Integer... keys) {
		// Press
		for(int i=0; i<keys.length; i++){
			robot.keyPress(keys[i]);
			sleep(100);
		}
		
		// release
		for(int i=keys.length-1; i>=0; i--){
			robot.keyRelease(keys[i]);
			sleep(100);
		}
	}
	
	public void altE() {
		robot.keyPress(KeyEvent.VK_ALT);
		robot.keyPress(KeyEvent.VK_E);
		robot.keyRelease(KeyEvent.VK_E);
		robot.keyRelease(KeyEvent.VK_ALT);
		sleep(defaultSleepAfterAction);
	}
	
	public void altE(int waitAfter) {
		altE();
		sleep(waitAfter);
	}
	
	public void altR() {
		robot.keyPress(KeyEvent.VK_ALT);
		robot.keyPress(KeyEvent.VK_R);
		robot.keyRelease(KeyEvent.VK_R);
		robot.keyRelease(KeyEvent.VK_ALT);
		sleep(defaultSleepAfterAction);
	}
	
	public void altR(int waitAfter) {
		altR();
		sleep(waitAfter);
	}
	
	public void altF() {
		robot.keyPress(KeyEvent.VK_ALT);
		robot.keyPress(KeyEvent.VK_F);
		robot.keyRelease(KeyEvent.VK_F);
		robot.keyRelease(KeyEvent.VK_ALT);
		sleep(defaultSleepAfterAction);
	}
	
	public void altF(int waitAfter) {
		altF();
		sleep(waitAfter);
	}
	
	public void altU() {
		robot.keyPress(KeyEvent.VK_ALT);
		robot.keyPress(KeyEvent.VK_U);
		robot.keyRelease(KeyEvent.VK_U);
		robot.keyRelease(KeyEvent.VK_ALT);
		sleep(defaultSleepAfterAction);
	}
	
	public void altU(int waitAfter) {
		altU();
		sleep(waitAfter);
	}
	
	
	
	public void altSpace() {
		robot.keyPress(KeyEvent.VK_ALT);
		robot.keyPress(KeyEvent.VK_SPACE);
		robot.keyRelease(KeyEvent.VK_SPACE);
		robot.keyRelease(KeyEvent.VK_ALT);
		sleep(defaultSleepAfterAction);
	}
	
	public void altSpace(int waitAfter) {
		altSpace();
		sleep(waitAfter);
	}
	
	public void altF4() {
		robot.keyPress(KeyEvent.VK_ALT);
		sleep(50);
		robot.keyPress(KeyEvent.VK_F4);
		sleep(50);
		robot.keyRelease(KeyEvent.VK_F4);
		sleep(50);
		robot.keyRelease(KeyEvent.VK_ALT);	
	}
	
	public void altF4(int waitAfter) {
		altF4();
		sleep(waitAfter);
	}
	
	public void controlC() {
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_C);
		robot.keyRelease(KeyEvent.VK_C);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		sleep(defaultSleepAfterAction);
	}
	
	public void controlC(int waitAfter) {
		controlC();
		sleep(waitAfter);
	}
	
	public void controlV() {
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_V);
		robot.keyRelease(KeyEvent.VK_V);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		sleep(defaultSleepAfterAction);
	}
	
	public void controlA() {
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_A);
		robot.keyRelease(KeyEvent.VK_A);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		sleep(defaultSleepAfterAction);
	}
	
	public void controlA(int waitAfter) {
		controlA();
		sleep(waitAfter);
	}
	
	public void controlF() {
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_F);
		robot.keyRelease(KeyEvent.VK_F);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		sleep(defaultSleepAfterAction);
	}
	
	public void controlF(int waitAfter) {
		controlF();
		sleep(waitAfter);
	}
	
	public void controlP() {
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_P);
		robot.keyRelease(KeyEvent.VK_P);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		sleep(defaultSleepAfterAction);
	}
	
	public void controlP(int waitAfter) {
		controlP();
		sleep(waitAfter);
	}
	
	public void controlU() {
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_U);
		robot.keyRelease(KeyEvent.VK_U);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		sleep(defaultSleepAfterAction);
	}
	
	public void controlU(int waitAfter) {
		controlU();
		sleep(waitAfter);
	}
	
	public void controlR() {
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_R);
		robot.keyRelease(KeyEvent.VK_R);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		sleep(defaultSleepAfterAction);
	}
	
	public void controlR(int waitAfter) {
		controlR();
		sleep(waitAfter);
	}
	
	public void controlT() {
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_T);
		robot.keyRelease(KeyEvent.VK_T);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		sleep(defaultSleepAfterAction);
	}
	
	public void controlT(int waitAfter) {
		controlT();
		sleep(waitAfter);
	}
	
	public void controlEnd() {
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_END);
		robot.keyRelease(KeyEvent.VK_END);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		sleep(defaultSleepAfterAction);
	}
	
	public void controlEnd(int waitAfter) {
		controlEnd();
		sleep(waitAfter);
	}
	
	public void controlHome() {
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_HOME);
		robot.keyRelease(KeyEvent.VK_HOME);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		sleep(defaultSleepAfterAction);
	}
	
	public void controlW() {
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_W);
		robot.keyRelease(KeyEvent.VK_W);
		robot.keyRelease(KeyEvent.VK_CONTROL);
	}
	
	public void controlW(int waitAfter) {
		controlW();
		sleep(waitAfter);
	}
	
	public void controlShiftP() {
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_SHIFT);
		robot.keyPress(KeyEvent.VK_P);
		robot.keyRelease(KeyEvent.VK_P);
		robot.keyRelease(KeyEvent.VK_SHIFT);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		sleep(defaultSleepAfterAction);
	}
	
	public void controlShiftP(int waitAfter) {
		controlShiftP();
		sleep(waitAfter);
	}
	
	public void controlShiftJ() {
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_SHIFT);
		robot.keyPress(KeyEvent.VK_J);
		robot.keyRelease(KeyEvent.VK_J);
		robot.keyRelease(KeyEvent.VK_SHIFT);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		sleep(defaultSleepAfterAction);
	}
	
	public void controlShiftJ(int waitAfter) {
		controlShiftJ();
		sleep(waitAfter);
	}
	
	public void shiftTab() {
		robot.keyPress(KeyEvent.VK_SHIFT);
		robot.keyPress(KeyEvent.VK_TAB);
		robot.keyRelease(KeyEvent.VK_TAB);
		robot.keyRelease(KeyEvent.VK_SHIFT);
		sleep(defaultSleepAfterAction);
	}
	
	public void shiftTab(int waitAfter) {
		shiftTab();
		sleep(waitAfter);
	}
	
	public String getClipboard() {
		try {
			Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
			Transferable t = clip.getContents(this);
			return new String(((String) t.getTransferData(DataFlavor.stringFlavor)).getBytes(), "UTF-8");
		} catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	

	public void typeLeft(int waitAfter) {
		typeLeft();
		sleep(waitAfter);
	}
	
	public void typeLeft() {
		robot.keyPress(KeyEvent.VK_LEFT);
		robot.keyRelease(KeyEvent.VK_LEFT);
		sleep(defaultSleepAfterAction);
	}
	
	public void typeDown(int waitAfter) {
		typeDown();
		sleep(waitAfter);
	}
	
	public void typeDown() {
		robot.keyPress(KeyEvent.VK_DOWN);
		robot.keyRelease(KeyEvent.VK_DOWN);
		sleep(defaultSleepAfterAction);
	}
	
	public void typeUp(int waitAfter) {
		typeUp();
		sleep(waitAfter);
	}
	
	public void typeUp() {
		robot.keyPress(KeyEvent.VK_UP);
		robot.keyRelease(KeyEvent.VK_UP);
		sleep(defaultSleepAfterAction);
	}
	
	
	public void typeWindows() {
		robot.keyPress(KeyEvent.VK_WINDOWS);
		robot.keyRelease(KeyEvent.VK_WINDOWS);
		sleep(defaultSleepAfterAction);
	}
	
	public void typeWindows(int waitAfter) {
		typeWindows();
		sleep(waitAfter);
	}
	
	public void space() {
		robot.keyPress(KeyEvent.VK_SPACE);
		robot.keyRelease(KeyEvent.VK_SPACE);
		sleep(defaultSleepAfterAction);
	}
	
	public void space(int waitAfter) {
		space();
		sleep(waitAfter);
	}
	
	public void backspace() {
		robot.keyPress(KeyEvent.VK_BACK_SPACE);
		robot.keyRelease(KeyEvent.VK_BACK_SPACE);
		sleep(defaultSleepAfterAction);
	}
	
	public void backspace(int waitAfter) {
		backspace();
		sleep(waitAfter);
		sleep(defaultSleepAfterAction);
	}
	
	public MarvinImage getScreenShot() {
		screenshot();
		return screen;
	}
	
	private void screenshot() {
		screen.setBufferedImage(robot.createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize())));
	}
	
	public MarvinImage screenCut(int x, int y, int width, int height) {
		MarvinImage img = new MarvinImage(robot.createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize())));
		MarvinImage imgOut = new MarvinImage(width, height);
		crop(img, imgOut, x, y, width, height);
		return imgOut;
	}
	
	
	
	public void saveScreenshot(String path) {
		screenshot();
		MarvinImageIO.saveImage(screen, path);
	}
	
	public void startRun(String command) {
		robot.keyPress(KeyEvent.VK_WINDOWS);
		sleep(1000);
		robot.keyPress(KeyEvent.VK_R);
		sleep(300);
		robot.keyRelease(KeyEvent.VK_R);
		sleep(300);
		robot.keyRelease(KeyEvent.VK_WINDOWS);
		sleep(100);
		type(command);
		sleep(3000);
		enter();
	}
	
	public void print(String text) {
		System.out.println(text);
	}
	
	public void wait(int ms) {
		sleep(ms);
	}
	
	private void sleep(int sleep) {
		try {	
			Thread.sleep(sleep);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void typeKey(char c) {
		int code = KeyEvent.getExtendedKeyCodeForChar(c);
		
		if((int) c >= 65 && (int)c <= 90) {
			robot.keyPress(KeyEvent.VK_SHIFT);
		}
		
		switch(c) {
			case 'á':
				robot.keyPress(KeyEvent.VK_DEAD_ACUTE);
				typeKey('a');
				return;
			case 'à':
				robot.keyPress(KeyEvent.VK_ALT);
				robot.keyPress(KeyEvent.VK_NUMPAD1);
				robot.keyRelease(KeyEvent.VK_NUMPAD1);
				robot.keyPress(KeyEvent.VK_NUMPAD3);
				robot.keyRelease(KeyEvent.VK_NUMPAD3);
				robot.keyPress(KeyEvent.VK_NUMPAD3);
				robot.keyRelease(KeyEvent.VK_NUMPAD3);
				robot.keyRelease(KeyEvent.VK_ALT);
				return;
			case 'ã':
				robot.keyPress(KeyEvent.VK_DEAD_TILDE);
				typeKey('a');
				return;
			case 'Ã':
				robot.keyPress(KeyEvent.VK_DEAD_TILDE);
				typeKey('A');
				return;
			case 'é':
				robot.keyPress(KeyEvent.VK_DEAD_ACUTE);
				typeKey('e');
				return;
			case 'ê':
				robot.keyPress(KeyEvent.VK_ALT);
				robot.keyPress(KeyEvent.VK_NUMPAD1);
				robot.keyRelease(KeyEvent.VK_NUMPAD1);
				robot.keyPress(KeyEvent.VK_NUMPAD3);
				robot.keyRelease(KeyEvent.VK_NUMPAD3);
				robot.keyPress(KeyEvent.VK_NUMPAD6);
				robot.keyRelease(KeyEvent.VK_NUMPAD6);
				robot.keyRelease(KeyEvent.VK_ALT);
				return;
			case 'í':
				robot.keyPress(KeyEvent.VK_DEAD_ACUTE);
				typeKey('i');
				return;
			case 'ç':
				robot.keyPress(KeyEvent.VK_ALT);
				robot.keyPress(KeyEvent.VK_NUMPAD1);
				robot.keyRelease(KeyEvent.VK_NUMPAD1);
				robot.keyPress(KeyEvent.VK_NUMPAD3);
				robot.keyRelease(KeyEvent.VK_NUMPAD3);
				robot.keyPress(KeyEvent.VK_NUMPAD5);
				robot.keyRelease(KeyEvent.VK_NUMPAD5);
				robot.keyRelease(KeyEvent.VK_ALT);
				return;
			case 'Ç':
				robot.keyPress(KeyEvent.VK_ALT);
				robot.keyPress(KeyEvent.VK_NUMPAD1);
				robot.keyRelease(KeyEvent.VK_NUMPAD1);
				robot.keyPress(KeyEvent.VK_NUMPAD2);
				robot.keyRelease(KeyEvent.VK_NUMPAD2);
				robot.keyPress(KeyEvent.VK_NUMPAD8);
				robot.keyRelease(KeyEvent.VK_NUMPAD8);
				robot.keyRelease(KeyEvent.VK_ALT);
				return;
			case ':':
				robot.keyPress(KeyEvent.VK_SHIFT);
				robot.keyPress(KeyEvent.VK_SEMICOLON);
				robot.keyRelease(KeyEvent.VK_SEMICOLON);
				robot.keyRelease(KeyEvent.VK_SHIFT);
				return;
			case '/':
				robot.keyPress(KeyEvent.VK_ALT);
				robot.keyPress(KeyEvent.VK_NUMPAD4);
				robot.keyRelease(KeyEvent.VK_NUMPAD4);
				robot.keyPress(KeyEvent.VK_NUMPAD7);
				robot.keyRelease(KeyEvent.VK_NUMPAD7);
				robot.keyRelease(KeyEvent.VK_ALT);
				return;
			case '&':
				robot.keyPress(KeyEvent.VK_ALT);
				robot.keyPress(KeyEvent.VK_NUMPAD3);
				robot.keyRelease(KeyEvent.VK_NUMPAD3);
				robot.keyPress(KeyEvent.VK_NUMPAD8);
				robot.keyRelease(KeyEvent.VK_NUMPAD8);
				robot.keyRelease(KeyEvent.VK_ALT);
				return;
			case '@':
				robot.keyPress(KeyEvent.VK_ALT);
				robot.keyPress(KeyEvent.VK_NUMPAD6);
				robot.keyRelease(KeyEvent.VK_NUMPAD6);
				robot.keyPress(KeyEvent.VK_NUMPAD4);
				robot.keyRelease(KeyEvent.VK_NUMPAD4);
				robot.keyRelease(KeyEvent.VK_ALT);
				return;
			case '$':
				robot.keyPress(KeyEvent.VK_ALT);
				robot.keyPress(KeyEvent.VK_NUMPAD3);
				robot.keyRelease(KeyEvent.VK_NUMPAD3);
				robot.keyPress(KeyEvent.VK_NUMPAD6);
				robot.keyRelease(KeyEvent.VK_NUMPAD6);
				robot.keyRelease(KeyEvent.VK_ALT);
				return;
			case '%':
				robot.keyPress(KeyEvent.VK_ALT);
				robot.keyPress(KeyEvent.VK_NUMPAD3);
				robot.keyRelease(KeyEvent.VK_NUMPAD3);
				robot.keyPress(KeyEvent.VK_NUMPAD7);
				robot.keyRelease(KeyEvent.VK_NUMPAD7);
				robot.keyRelease(KeyEvent.VK_ALT);
				return;
			case '?':
				robot.keyPress(KeyEvent.VK_ALT);
				robot.keyPress(KeyEvent.VK_NUMPAD6);
				robot.keyRelease(KeyEvent.VK_NUMPAD6);
				robot.keyPress(KeyEvent.VK_NUMPAD3);
				robot.keyRelease(KeyEvent.VK_NUMPAD3);
				robot.keyRelease(KeyEvent.VK_ALT);
				return;
			case '_':
				robot.keyPress(KeyEvent.VK_SHIFT);
				robot.keyPress(KeyEvent.VK_MINUS);
				robot.keyRelease(KeyEvent.VK_MINUS);
				robot.keyRelease(KeyEvent.VK_SHIFT);
				return;
			case '(':
				robot.keyPress(KeyEvent.VK_ALT);
				robot.keyPress(KeyEvent.VK_NUMPAD4);
				robot.keyPress(KeyEvent.VK_NUMPAD0);
				robot.keyRelease(KeyEvent.VK_NUMPAD0);
				robot.keyRelease(KeyEvent.VK_NUMPAD4);
				robot.keyRelease(KeyEvent.VK_ALT);
				return;
			case ')':
				robot.keyPress(KeyEvent.VK_ALT);
				robot.keyPress(KeyEvent.VK_NUMPAD4);
				robot.keyPress(KeyEvent.VK_NUMPAD1);
				robot.keyRelease(KeyEvent.VK_NUMPAD1);
				robot.keyRelease(KeyEvent.VK_NUMPAD4);
				robot.keyRelease(KeyEvent.VK_ALT);
				return;
		}
		
		
//		System.out.println("char:"+c);
		robot.keyPress(code);
		robot.keyRelease(code);
		robot.keyRelease(KeyEvent.VK_SHIFT);
	}
	
	private Point getElementCoords(MarvinImage sub, double matching) {
		return getElementCoords(sub, 0, 0, screen.getWidth(), screen.getHeight(), matching);
	}
	
	private Point getElementCoords
	(
		MarvinImage sub, 
		int startX, 
		int startY,
		int searchWindowWidth,
		int searchWindowHeight,
		double matching) {
		return getElementCoords(sub, screen, startX, startY, searchWindowWidth, searchWindowHeight, matching);
	}
	
	private Point getElementCoords(MarvinImage sub, MarvinImage screen, double matching) {
		return getElementCoords(sub, screen, 0, 0, screen.getWidth(), screen.getHeight(), matching);
	}
	
	private Point getElementCoords
	(
			MarvinImage sub, 
			MarvinImage screen, 
			int startX, 
			int startY,
			int searchWindowWidth,
			int searchWindowHeight,
			double matching
	) {
		long time=System.currentTimeMillis();
		MarvinSegment seg = findSubimage(sub, screen, startX, startY, searchWindowWidth, searchWindowHeight, matching, false);
		//System.out.println("search time:"+(System.currentTimeMillis()-time));
		
		if(seg != null) {
			return new Point(seg.x1,seg.y1);
		}
		return null;
	}
	
	private Point getElementCoordsCentered(MarvinImage sub, double matching) {
		Point p = getElementCoords(sub, matching);
		
		if(p != null) {
			int x = p.x + (sub.getWidth() / 2);
			int y = p.y + (sub.getHeight() / 2);
			return new Point(x,y);
		}
		return null;
	}
	
	//findSubimage(sub, screen, startX, startY, matching);
	
	
	public MarvinSegment findSubimage
	(
		MarvinImage subimage,
		MarvinImage imageIn,
		int startX,
		int startY,
		Double similarity,
		boolean findAll
	) {
		return findSubimage(subimage, imageIn, startX, startY, imageIn.getWidth(), imageIn.getHeight(), similarity, findAll);
	}
	
	
	public MarvinSegment findSubimage
	(
		MarvinImage subimage,
		MarvinImage imageIn,
		int startX,
		int startY,
		int searchWindowWidth,
		int searchWindowHeight,
		Double similarity,
		boolean findAll
	) {
		List<MarvinSegment> segments = new ArrayList<MarvinSegment>();
		int subImagePixels = subimage.getWidth()*subimage.getHeight();
		boolean[][] processed=new boolean[imageIn.getWidth()][imageIn.getHeight()];
		
		int r1,g1,b1,r2,g2,b2;
		// Full image
		mainLoop:for(int y=startY; y<startY+searchWindowHeight; y++){
			for(int x=startX; x<startX+searchWindowWidth; x++){
				
				if(processed[x][y]){
					continue;
				}
				
				int notMatched=0;
				boolean match=true;
				// subimage
				if(y+subimage.getHeight() < imageIn.getHeight() && x+subimage.getWidth() < imageIn.getWidth()){
				
					
					outerLoop:for(int i=0; i<subimage.getHeight(); i++){
						for(int j=0; j<subimage.getWidth(); j++){
							
							if(processed[x+j][y+i]){
								match=false;
								break outerLoop;
							}
							
							r1 = imageIn.getIntComponent0(x+j, y+i);
							g1 = imageIn.getIntComponent1(x+j, y+i);
							b1 = imageIn.getIntComponent2(x+j, y+i);
							
							r2 = subimage.getIntComponent0(j, i);
							g2 = subimage.getIntComponent1(j, i);
							b2 = subimage.getIntComponent2(j, i);
							
							if
							(
								Math.abs(r1-r2) > 5 ||
								Math.abs(g1-g2) > 5 ||
								Math.abs(b1-b2) > 5
							){
								notMatched++;
								
								if(notMatched > (1-similarity)*subImagePixels){
									match=false;
									break outerLoop;
								}
							}
						}
					}
				} else{
					match=false;
				}
				
				if(match){
					segments.add(new MarvinSegment(x,y,x+subimage.getWidth(), y+subimage.getHeight()));
					
					if(!findAll){
						break mainLoop;
					}
					
					for(int i=0; i<subimage.getHeight(); i++){
						for(int j=0; j<subimage.getWidth(); j++){
							processed[x+j][y+i]=true;
						}
					}
					
				}
			}
		}
		
		if(!segments.isEmpty()) {
			if(!findAll) {
				return segments.get(0);
			}
		}
		
		return null;
	}
}