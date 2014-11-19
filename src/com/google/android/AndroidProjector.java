package com.google.android;


import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import com.android.ddmlib.RawImage;

public class AndroidProjector
{
  private Label mImageLabel;
  private RawImage mRawImage;
  private boolean mRotateImage = false;
  private static final String ADB_HOST = "127.0.0.1";
  private static final int ADB_PORT = 5037;
  private static final int WAIT_TIME = 5;
  private static int percentSize = 100;
  private static int width = 500;
  private static int height = 795;
  private static int widthImage = 480;
  private static int heightImage = 768;
  
  private void open()
    throws IOException
  {
    Display.setAppName("Android Projector");
    Display localDisplay = new Display();
    final Shell localShell = new Shell(localDisplay);
    localShell.setText("Device Screen");
    localShell.setSize(width,height);
    //localShell.setLocation(300, 300);
    createContents(localShell);
    localShell.addShellListener(new ShellListener() {

        public void shellIconified(ShellEvent e) {
        }
        public void shellDeiconified(ShellEvent e) {
        }
        public void shellDeactivated(ShellEvent e) {
        }
        public void shellClosed(ShellEvent e) {
            System.out.println("Client Area: " + localShell.getClientArea());
        }
        public void shellActivated(ShellEvent e) {
            int frameX = localShell.getSize().x - localShell.getClientArea().width;
            int frameY = localShell.getSize().y - localShell.getClientArea().height;
            if (AndroidProjector.this.mRotateImage) { 
            	localShell.setSize(height * percentSize/100 + frameX, width * percentSize/100 + frameY);
            }
            else { 
            	localShell.setSize(width * percentSize/100 + frameY,height * percentSize/100); 
            }
        }
    });     
    localShell.open();
    SocketChannel localSocketChannel = null;
    try
    {
      while (!localShell.isDisposed()) {
        if (!localDisplay.readAndDispatch())
        {
          localSocketChannel = connectAdbDevice();
          if (localSocketChannel == null) {
            break;
          }
          if (startFramebufferRequest(localSocketChannel))
          {
        	int frameX = localShell.getSize().x - localShell.getClientArea().width;
            int frameY = localShell.getSize().y - localShell.getClientArea().height;
            getFramebufferData(localSocketChannel);
            updateDeviceImage(localShell, this.mRotateImage ? this.mRawImage.getRotated() : this.mRawImage);
            if (this.mRotateImage) { 
            	localShell.setSize(height * percentSize/100 + frameX, width * percentSize/100 + frameY);
            }
            else { 
            	localShell.setSize(width * percentSize/100 + frameX, height * percentSize/100 + frameY);
            }
          }
          localSocketChannel.close();
        }
      }
    }
    finally
    {
      if (localSocketChannel != null) {
        localSocketChannel.close();
      }
      localDisplay.dispose();
    }
  }
  
  private void createContents(Shell paramShell)
  {
    Menu localMenu1 = new Menu(paramShell, 2);
    MenuItem localMenuItem1 = new MenuItem(localMenu1, 64);
    localMenuItem1.setText("&View");
    Menu localMenu2 = new Menu(localMenu1);
    localMenuItem1.setMenu(localMenu2);
    MenuItem localMenuItem2 = new MenuItem(localMenu2, 16);
    MenuItem localMenuItem3 = new MenuItem(localMenu2, 16);
    localMenuItem2.setText("Portrait");
    localMenuItem2.setSelection(true);
    localMenuItem2.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent paramAnonymousSelectionEvent)
      {
        AndroidProjector.this.mRotateImage = false;
      }
    });
    localMenuItem3.setText("Landscape");
    localMenuItem3.setSelection(false);
    localMenuItem3.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent paramAnonymousSelectionEvent)
      {
        AndroidProjector.this.mRotateImage = true;
      }
    });
    MenuItem localMenuItem4 = new MenuItem(localMenu1,64);
    localMenuItem4.setText("&Zoom");
    Menu localMenu3 = new Menu(localMenu1);
    localMenuItem4.setMenu(localMenu3);
    MenuItem localMenuItem5 = new MenuItem(localMenu3,16);
    MenuItem localMenuItem6 = new MenuItem(localMenu3,16);
    MenuItem localMenuItem7 = new MenuItem(localMenu3,16);
    localMenuItem5.setText("100%");
    localMenuItem5.setSelection(true);
    localMenuItem5.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent paramAnonymousSelectionEvent)
      {
    	  //AndroidProjector.this.mRawImage.setImageSize(100);
    	  percentSize = 100;
      }
    });
    localMenuItem6.setText("80%");
    localMenuItem6.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent paramAnonymousSelectionEvent)
      {
    	  //AndroidProjector.this.mRawImage.setImageSize(80);
    	  percentSize = 80;
      }
    });
    localMenuItem7.setText("60%");
    localMenuItem7.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent paramAnonymousSelectionEvent)
      {
    	  //AndroidProjector.this.mRawImage.setImageSize(60);
    	  percentSize = 60;
      }
    });
    
    paramShell.setMenuBar(localMenu1);
    paramShell.setLayout(new FillLayout());
    this.mImageLabel = new Label(paramShell, 2048);
    this.mImageLabel.pack();
    paramShell.pack();
  }
  
  private SocketChannel connectAdbDevice()
    throws IOException
  {
    InetAddress localInetAddress;
    try
    {
      localInetAddress = InetAddress.getByName("127.0.0.1");
    }
    catch (UnknownHostException localUnknownHostException)
    {
      return null;
    }
    InetSocketAddress localInetSocketAddress = new InetSocketAddress(localInetAddress, 5037);
    SocketChannel localSocketChannel = SocketChannel.open(localInetSocketAddress);
    localSocketChannel.configureBlocking(false);
    sendAdbRequest(localSocketChannel, "host:transport-usb");
    if (!checkAdbResponse(localSocketChannel)) {
      return null;
    }
    return localSocketChannel;
  }
  
  private boolean startFramebufferRequest(SocketChannel paramSocketChannel)
    throws IOException
  {
    sendAdbRequest(paramSocketChannel, "framebuffer:");
    if (checkAdbResponse(paramSocketChannel))
    {
      getFramebufferHeader(paramSocketChannel);
      return true;
    }
    return false;
  }
  
  private void getFramebufferHeader(SocketChannel paramSocketChannel)
    throws IOException
  {
    ByteBuffer localByteBuffer = ByteBuffer.wrap(new byte[4]);
    readAdbChannel(paramSocketChannel, localByteBuffer);
    localByteBuffer.rewind();
    localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    int i = localByteBuffer.getInt();
    int j = RawImage.getHeaderSize(i);
    localByteBuffer = ByteBuffer.wrap(new byte[j * 4]);
    readAdbChannel(paramSocketChannel, localByteBuffer);
    localByteBuffer.rewind();
    localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    this.mRawImage = new RawImage();
    this.mRawImage.readHeader(i, localByteBuffer);
  }
  
  private void getFramebufferData(SocketChannel paramSocketChannel)
    throws IOException
  {
    byte[] arrayOfByte1 = { 0 };
    ByteBuffer localByteBuffer = ByteBuffer.wrap(arrayOfByte1);
    writeAdbChannel(paramSocketChannel, localByteBuffer);
    byte[] arrayOfByte2 = new byte[this.mRawImage.size];
    localByteBuffer = ByteBuffer.wrap(arrayOfByte2);
    readAdbChannel(paramSocketChannel, localByteBuffer);
    this.mRawImage.data = arrayOfByte2;
    
  }
  
  private void sendAdbRequest(SocketChannel paramSocketChannel, String paramString)
    throws IOException
  {
    String str = String.format("%04X%s", new Object[] { Integer.valueOf(paramString.length()), paramString });
    ByteBuffer localByteBuffer = ByteBuffer.wrap(str.getBytes());
    writeAdbChannel(paramSocketChannel, localByteBuffer);
  }
  
  private boolean checkAdbResponse(SocketChannel paramSocketChannel)
    throws IOException
  {
    ByteBuffer localByteBuffer = ByteBuffer.wrap(new byte[4]);
    readAdbChannel(paramSocketChannel, localByteBuffer);
    return (localByteBuffer.array()[0] == 79) && (localByteBuffer.array()[3] == 89);
  }
  
  private void writeAdbChannel(SocketChannel paramSocketChannel, ByteBuffer paramByteBuffer)
    throws IOException
  {
    while (paramByteBuffer.position() != paramByteBuffer.limit())
    {
      int i = paramSocketChannel.write(paramByteBuffer);
      if (i < 0) {
        throw new IOException("EOF");
      }
      if (i == 0) {
        try
        {
          Thread.sleep(5L);
        }
        catch (InterruptedException localInterruptedException) {}
      }
    }
  }
  
  private void readAdbChannel(SocketChannel paramSocketChannel, ByteBuffer paramByteBuffer)
    throws IOException
  {
    while (paramByteBuffer.position() != paramByteBuffer.limit())
    {
      int i = paramSocketChannel.read(paramByteBuffer);
      if (i < 0) {
        throw new IOException("EOF");
      }
      if (i == 0) {
        try
        {
          Thread.sleep(5L);
        }
        catch (InterruptedException localInterruptedException) {}
      }
    }
  }
  
  private void updateDeviceImage(Shell paramShell, RawImage paramRawImage)
  {
    PaletteData localPaletteData = new PaletteData(paramRawImage.getRedMask(), paramRawImage.getGreenMask(), paramRawImage.getBlueMask());
    
    ImageData localImageData = null;
    localImageData = new ImageData(paramRawImage.width, paramRawImage.height, paramRawImage.bpp, localPaletteData, 1, paramRawImage.data);
    /*System.out.print("Width:" + Integer.valueOf(paramRawImage.width).toString() + "\n" );
    System.out.print("Height:" + Integer.valueOf(paramRawImage.height) + "\n"); 
    System.out.print("GreenMask:" + Integer.valueOf(paramRawImage.getGreenMask()) + "\n");
    System.out.print("BlueMask:" + Integer.valueOf(paramRawImage.getBlueMask()) + "\n");
    System.out.print("RedMask:" + Integer.valueOf(paramRawImage.getRedMask()) + "\n"); */
    /*
     * GreenMask:16711680
BlueMask:65280
RedMask:-16777216
Width:480
Height:768
GreenMask:-536412160
BlueMask:520093696
RedMask:16252928
     */
    try { 
    Image localImage = new Image(paramShell.getDisplay(), localImageData);
    
    if (this.mRotateImage) { 
    	
    	localImage = resize(localImage,heightImage * percentSize/100 ,widthImage * percentSize/100);
    } else { 
    	localImage = resize(localImage,widthImage * percentSize/100,heightImage * percentSize/100);
    }
    this.mImageLabel.setImage(localImage);
    this.mImageLabel.pack();
    paramShell.pack();
    } catch (Exception e) {
    	String fullStackTrace = org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(e);
    	//System.out.print(e.toString());
    	System.out.print(fullStackTrace);
    	//
    } /*finally { 
    	System.exit(0);
    } */
  }
  
  public static void main(String[] paramArrayOfString)
  {
    AndroidProjector localAndroidProjector = new AndroidProjector();
    try
    {
      localAndroidProjector.open();
    }
    catch (IOException localIOException) {}
  }
  

  


  
  private Image resize(Image image, int width, int height) {
	  Image scaled = new Image(Display.getDefault(), width, height);
	  GC gc = new GC(scaled);
	  gc.setAntialias(SWT.ON);
	  gc.setInterpolation(SWT.HIGH);
	  gc.drawImage(image, 0, 0,  image.getBounds().width, image.getBounds().height, 
	  0, 0, width, height);
	  gc.dispose();
	  image.dispose(); // don't forget about me!
	  return scaled;
	  }
}



