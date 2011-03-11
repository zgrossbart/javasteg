/*******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/
package stegtest;

import java.util.*;

import java.io.*;
import java.text.SimpleDateFormat;
import java.net.URL;
import java.net.MalformedURLException;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.imageio.*;
import javax.imageio.plugins.jpeg.*;
import javax.imageio.stream.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;

public class StegTest extends JPanel implements ActionListener 
{
    /*
     * This is the singleton of our application.
     */
    private static StegTest ST;

    /*
     * This is the frame of our application.
     */
    private JFrame m_mainFrame;

    /*
     * This is the path to the original image.
     */
    private String m_imgPath;

    /*
     * This is the original image.
     */
    private Image m_image;

    /*
     * This is the width of our image.
     */
    private int m_imgWidth;

    /*
     * This is it height of our image.
     */
    private int m_imgHeight;

    /*
     * This is the label for the original image.
     */
    private JLabel m_lblImageLabel;

    /*
     * This is the label which shows the original image.
     */
    private JLabel m_lblImage;

    /*
     * This is where the user will type the message to encode.
     */
    private JTextArea m_message;

    /*
     * This is the button used to start the encoding process.
     */
    private JButton m_encodeBtn;

    /*
     * This label shows the image with the encoded message.
     */
    private JLabel m_lblEncodedImage;
    
    /*
     * This label shows the extracted message.
     */
    private JTextArea m_extractedMessage;

    /*
     * This is our modified image with the encoded message.
     */
    private Image m_modImg;

    /*
     * When we encode our write our string onto the image we need to
     * change only the last two bits of the byte.  When we read the 
     * String back we need to read only the last two bits of the byte.
     * The problem is that those last two bits might already have 
     * data in them.  This mask will clear out the data in the last
     * two bits when we write and clear out all the rest of the data
     * when we read.
     */
    private static final int LSB_MASK_READ = 0x03;
    private static final int LSB_MASK_WRITE = 0xFF & ~LSB_MASK_READ;         
    
    public static StegTest getStegTest()
    {
        return ST;
    }
    
    /*
     * These two arrays are the list of variables we will pass when each thread
     * runs. You can change these parameters to be whatever you want.
     */
    public static void main(String args[])
    {
        setNativeLookAndFeel();

        JFrame mainFrame = new JFrame();
        mainFrame = new JFrame("Steganography Test");
        mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        String img = null;
        if (args.length > 0) {
            /*
             * If the user wants to pass an image path on the command line
             * they can save a little typing.
             */
            img = args[0];
        }

        ST = new StegTest(mainFrame, img);
        ST.init();

        mainFrame.getContentPane().setLayout(new BorderLayout());
        mainFrame.getContentPane().add(ST, BorderLayout.CENTER);

        mainFrame.pack();
        mainFrame.setVisible(true);
    }

    public static void setNativeLookAndFeel()
    {
        /*
         * Things look a little better in the native look and feel.
         */
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("Error setting native look and feel: " + e);
        }
    }

    /**
     * Create a new StegTest object.  This method is private since this class is a
     * singleton.
     * 
     * @param mainFrame the frame this panel will be in
     * @param imgPath   the path to an image to open - may be null
     */
    private StegTest(JFrame mainFrame, String imgPath)
    {
        m_mainFrame = mainFrame;
        m_imgPath = imgPath;
    }

    /*
     * This method will initialize the UI and create all of the Swing
     * controls.
     */
    private void init()
    {
        setOpaque(true);

        GridBagLayout layout = new GridBagLayout();
        setLayout(layout);

        /*
         * This label will show the introduction text for the application.
         */
        JTextArea introLabel = new JTextArea();
        introLabel.setEditable(false);
        introLabel.setFocusable(false);
        introLabel.setLineWrap(true);
        introLabel.setMargin(new Insets(5, 5, 5, 5));
        introLabel.setWrapStyleWord(true);
        introLabel.setBorder(new LineBorder(Color.black));
        introLabel.setPreferredSize(new Dimension(200, 400));
        add(introLabel);
        introLabel.setText("Steganography Test is a sample program which will encode a message in an image " + 
                           "file and decode that message at a later point.  You can use this program to " + 
                           "open an image to encode, encode a message in that image, save the resulting " + 
                           "image, and retrieve the messages encoded in images.  You can access most of " + 
                           "these functions using the File menu.\n\n" + 
                           "This program will read in JPEG, GIF, and PNG images, but it will only write " + 
                           "PNG images.");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weighty = 0.0001;
        gbc.weightx = 0.0001;
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.gridheight = GridBagConstraints.REMAINDER;
        gbc.gridwidth = 1;
        layout.setConstraints(introLabel, gbc);

        JPanel imgPanel = createImagePanel();
        add(imgPanel);
        gbc.gridx++;
        gbc.weightx = 0.0001;
        layout.setConstraints(imgPanel, gbc);

        JPanel msgPanel = createMessagePanel();
        add(msgPanel);
        gbc.gridx++;
        gbc.weightx = 0.65;
        layout.setConstraints(msgPanel, gbc);
        
        /*
         * Create the menu bar.
         */
        new Actions().initMenus(m_mainFrame);
    }

    private JPanel createMessagePanel()
    {
        JPanel msgPanel = new JPanel();
        GridBagLayout layout = new GridBagLayout();
        msgPanel.setLayout(layout);

        /*
         * This is the text field to enter the message to encode.
         */
        JLabel messageLabel = new JLabel("Message to Encode:");
        msgPanel.add(messageLabel);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weighty = 0.0001;
        gbc.weightx = 0.0001;
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.gridheight = 1;
        gbc.gridwidth = 1;
        layout.setConstraints(messageLabel, gbc);

        m_message = new JTextArea();
        m_message.setMargin(new Insets(5, 5, 5, 5));
        m_message.setBorder(new LineBorder(Color.black));
        
        JScrollPane scroll = new JScrollPane(m_message);
        scroll.setPreferredSize(new Dimension(200, 200));
        msgPanel.add(scroll);
        gbc.gridy++;
        gbc.weighty = 0.9;
        layout.setConstraints(scroll, gbc);

        /*
         * The button to do the actual encoding.
         */
        m_encodeBtn = new JButton("Create Encoded Image");
        msgPanel.add(m_encodeBtn);
        m_encodeBtn.addActionListener(this);
        gbc.gridy++;
        gbc.weighty = 0.0001;
        layout.setConstraints(m_encodeBtn, gbc);

        /*
         * This text area will show the extracted message
         */
        JLabel decodedMessageLabel = new JLabel("Decoded Message:");
        msgPanel.add(decodedMessageLabel);
        gbc.gridy++;
        layout.setConstraints(decodedMessageLabel, gbc);
        
        m_extractedMessage = new JTextArea();
        m_extractedMessage.setEditable(false);
        m_extractedMessage.setFocusable(true);
        m_extractedMessage.setMargin(new Insets(5, 5, 5, 5));
        m_extractedMessage.setBorder(new LineBorder(Color.black));
        
        scroll = new JScrollPane(m_extractedMessage);
        scroll.setPreferredSize(new Dimension(200, 200));
        msgPanel.add(scroll);
        gbc.gridy++;
        gbc.weighty = 0.9;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        layout.setConstraints(scroll, gbc);

        return msgPanel;
    }

    private JPanel createImagePanel()
    {
        JPanel imgPanel = new JPanel();
        GridBagLayout layout = new GridBagLayout();
        imgPanel.setLayout(layout);
        
        /*
         * This label will show the original image before it has been changed.
         */
        m_lblImageLabel = new JLabel("The Original Image");
        imgPanel.add(m_lblImageLabel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weighty = 0.0001;
        gbc.weightx = 0.0001;
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.gridheight = 1;
        gbc.gridwidth = 1;
        layout.setConstraints(m_lblImageLabel, gbc);
        
        m_lblImage = new JLabel();
        imgPanel.add(m_lblImage);
        if (m_imgPath != null) {
            m_lblImage.setIcon(new ImageIcon(loadImage()));
        }
        gbc.gridy++;
        layout.setConstraints(m_lblImage, gbc);

        /*
         * This label shows the encoded image.  It should look the same
         * as the original image.
         */
        JLabel cImageLabel = new JLabel("The Encoded Image");
        imgPanel.add(cImageLabel);
        gbc.gridy++;
        layout.setConstraints(cImageLabel, gbc);

        m_lblEncodedImage = new JLabel();
        imgPanel.add(m_lblEncodedImage);
        gbc.gridy++;
        layout.setConstraints(m_lblEncodedImage, gbc);

        JPanel spacer = new JPanel();
        imgPanel.add(spacer);
        spacer.setOpaque(false);
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weighty = 0.9;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        layout.setConstraints(spacer, gbc);

        return imgPanel;
    }

    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == m_encodeBtn) {
            if (m_lblImage.getIcon() == null) {
                JOptionPane.showMessageDialog(this, "You have to open an image before encoding a message in it",
                                              "No Available Image", JOptionPane.WARNING_MESSAGE);
                return;
            }
            /*
             * This is am image where we will draw the original image.  This image will
             * never be drawn on the screen, but we need this to get the int array of 
             * the image.
             */
            BufferedImage buffImage = new BufferedImage(m_imgWidth, m_imgHeight, BufferedImage.TYPE_INT_ARGB);

            /*
             * Draw our image.
             */
            Graphics g = buffImage.getGraphics();
            g.drawImage(m_image, 0, 0, null);

            /*
             * Now we get the int array of our image.  These ints represent the red, green,
             * and blue values of each pixel in our image.  The pixels will be in the order
             * of each line of the image from left to right.  This means our array will have
             * a length equal to the width of our image multiplied by the height of our
             * image.  Each pixel is made up of 32 bits of data separated into four chunks.
             * The first chunk (bits 1-8) is the alpha data which we don't want to change.
             * The second chunk (bits 9-16) is the red information for the pixel.  The third
             * chunk (bits 17-24) is the green information for the pixel.  The fourth and last
             * chunk (bits 25-32) is the blue information for the pixel.
             */
            DataBufferInt dataBufferInt = (DataBufferInt)buffImage.getRaster().getDataBuffer();
            int imageData[] = dataBufferInt.getData();

            /*
             * Having all of the data in a large single length array is difficult to work with
             * so we are going to break up the array into some more easily changed parts.
             */
            int[][][] imageData3D = get3DArray(imageData, m_imgWidth, m_imgHeight);

            /*
             * Now that we have the three dimensional array we can create the encoded image
             * and convert it back to a one dimensional array.  This method will create a 
             * copy of the original image and change only the copy. 
             */
            int[][][] encodedImg = encodeMessage(imageData3D, m_imgWidth, m_imgHeight, m_message.getText());
            imageData = get1DArray(encodedImg, m_imgWidth, m_imgHeight);

            /*
             * Now that we have the data of our new image we want to create an actual image
             * out of it so we can display it to the user.
             */
            m_modImg = createImage(new MemoryImageSource(m_imgWidth, m_imgHeight, imageData, 0, m_imgWidth));
            m_lblEncodedImage.setIcon(new ImageIcon(m_modImg));

            /*
             * The encoded image will look just like the original image so we also want to 
             * get the message from the image and dispay it to the user so the user knows 
             * that everything worked well.
             * 
             */
            m_extractedMessage.setText(getMessage(encodedImg, m_imgWidth, m_imgHeight));

            /*
             * Last but not least we need to resize our frame since we added another image 
             * to it.
             */
            doLayout();
            m_mainFrame.pack();
        }
    }

    /*
     * This method will prompt the user to choose an image, read that image file, set it as
     * the icon for the original image label, and resize the frame so that the image is visible.
     */
    protected void openImage()
    {
        FileDialog dialog = new FileDialog(m_mainFrame, "Choose an Image File", FileDialog.LOAD);
        dialog.setFilenameFilter(new FilenameFilter() 
            {
                public boolean accept(File dir, String name)
                {
                    return name.endsWith(".jpg") ||
                        name.endsWith(".gif") ||
                        name.endsWith(".png");
                }
            });

        dialog.setVisible(true);

        String file = new File(dialog.getDirectory(), dialog.getFile()).getAbsolutePath();

        if (file != null) {
            m_imgPath = file;
            Image image = loadImage();
            if (image != null) {
                m_lblImage.setIcon(new ImageIcon(image));
            }
        }

        doLayout();
        m_mainFrame.pack();
    }

    /*
     * This method will prompt the user to open an image with an encoded message in it.  It will
     * then display that image, try to read a message from that image, and display the message if
     * there is one.
     */
    protected void getMessage()
    {
        FileDialog dialog = new FileDialog(m_mainFrame, "Choose A JPEG Image", FileDialog.LOAD);
        dialog.setFilenameFilter(new FilenameFilter() 
            {
                public boolean accept(File dir, String name)
                {
                    /*
                     * We only save PNG files.
                     */
                    return name.endsWith(".png");
                }
            });

        dialog.setVisible(true);

        String file = dialog.getFile();

        if (file != null) {
            m_imgPath = file;
            Image image = loadImage();
            if (image != null) {
                m_lblEncodedImage.setIcon(new ImageIcon(image));
            } else {
                /*
                 * This means they didn't select a valid image.
                 */
                return;
            }
        } else {
            /*
             * This means they cancelled the dialog.
             */
            return;
        }

        /*
         * Clear out the original image if one has been set.
         */
        m_lblImage.setIcon(null);

        /*
         * Now we will create a buffered image for use to get the message from.  We are creating
         * this buffered image so it uses the correct data format for us.
         */
        BufferedImage buffImage = new BufferedImage(m_imgWidth, m_imgHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics g = buffImage.getGraphics();
        g.drawImage(m_image, 0, 0, null);

        /*
         * Get the pixel data from the image.
         */
        DataBufferInt dataBufferInt = (DataBufferInt)buffImage.getRaster().getDataBuffer();
        int imageData[] = dataBufferInt.getData();

        /*
         * No we want to break the data up into the more managable three dimensional
         * array.
         */
        int[][][] imageData3D = get3DArray(imageData, m_imgWidth, m_imgHeight);

        /*
         * Then we will get the message out of the image pixel data.
         */
        String message = getMessage(imageData3D, m_imgWidth, m_imgHeight);

        if (message != null) {
            m_extractedMessage.setText(message);
        } else {
            m_extractedMessage.setText("This image did not contain an encoded message.");
        }

        /*
         * Last but not least we resize the frame to fit the new image we just loaded.
         */
        doLayout();
        m_mainFrame.pack();
    }

    /*
     * This method will prompt the user for a file and save the image with the encoded message.
     */
    protected void saveImage()
    {
        if (m_modImg == null) {
            /*
             * If they haven't created an encoded image than we have nothing to save.
             */
            JOptionPane.showMessageDialog(this, "You must create an encoded image before you can save it.", 
                                          "Unable to save", JOptionPane.WARNING_MESSAGE);
            return;
        }

        /*
         * Prompt them for a location to save their image.
         */
        FileDialog dialog = new FileDialog(m_mainFrame, "Choose A Location for Your Image", FileDialog.SAVE);
        dialog.setFilenameFilter(new FilenameFilter() 
            {
                public boolean accept(File dir, String name)
                {
                    /*
                     * We only support PNG images
                     */
                    return name.endsWith(".png");
                }
            });

        dialog.setVisible(true);

        String file = dialog.getFile();

        if (file == null) {
            /*
             * This means they cancelled.
             */
            return;
        }

        /*
         * We want to make sure the file has a .png extension
         */
        if (!file.endsWith(".png")) {
            file = file + ".png";
        }

        File f = new File(file);
        
        /*
         * This is the buffered image we will use to draw our image with the encoded message.  Up to 
         * this point we have always used TYPE_INT_ARGB.  We use that because it is an easy format
         * to parse the data.  However, TYPE_INT_ARGB will create a CMYK (Cyan, Magenta, Yellow, and
         * Black or key) image.  This image format doesn't work in most of the browsers and we want
         * to create an image using the more popular RGB (Red, Green, Blue) format.
         */
        BufferedImage buffImage = new BufferedImage(m_imgWidth, m_imgHeight, BufferedImage.TYPE_INT_RGB);
        Graphics g = buffImage.getGraphics();
        g.drawImage(m_modImg, 0, 0, null);

        /*
         * Now that we have the image in the buffer we just need to write it to the disk
         */
        try {
            FileOutputStream out = new FileOutputStream(f);
            try {
                ImageIO.write(buffImage, "png", out);
            } finally {
                /*
                 * We need to make sure to close our stream.
                 */
                if (out != null) {
                    out.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * This method will load the image to have a message encoded into it.
     */
    private Image loadImage()
    {
        if (m_imgPath == null) {
            /*
             * If there isn't an image we want to prompt the user to open one.
             */
            openImage();
        }

        /*
         * We'll change the label to show which image is loaded.
         */
        m_lblImageLabel.setText("The Original Image - " + m_imgPath);

        try {
            /*
             * Now we read the actual image into memory.
             */
            m_image = ImageIO.read(new File(m_imgPath));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        
        /*
         * If the image is large we want to wait for it to load.  We will use the MediaTracker
         * object to make sure that the image is fully loaded.  If the image is not fully 
         * loaded we can't get the width or the height of the image.
         */
        MediaTracker tracker = new MediaTracker(this);
        tracker.addImage(m_image, 1);

        try {
            if (!tracker.waitForID(1,10000)) {
                System.out.println("Unable to load image.");
                System.exit(1);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Unable to load the image at " + m_imgPath + ".", 
                                          "Unable To Load Image", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        /*
         * If the image wasn't loaded properly we will warn the user.
         */
        if ((tracker.statusAll(false) & MediaTracker.ERRORED & MediaTracker.ABORTED) != 0) {
            JOptionPane.showMessageDialog(this, "Unable to load the image at " + m_imgPath + ".", 
                                          "Unable To Load Image", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        /*
         * Last but not least we want to get the image's dimensions so we can use them later.
         */
        m_imgWidth = m_image.getWidth(this);
        m_imgHeight = m_image.getHeight(this);

        return m_image;
    }

    /*
     * This method will take a one dimension array and break it up into a three dimensional
     * array which is easier to work with.
     */
    private static int[][][] get3DArray(int[] imageData, int cols, int rows)
    {
        /*
         * This is the array which will be populated with the pixel
         * color data.  The array has a length of four with the 
         * elements alpha data, red, green, and blue.
         */
        int[][][] data = new int[rows][cols][4];

        for (int row = 0; row < rows; row++) {
            /*
             * We'll deal with each row separately.
             */
            int[] aRow = new int[cols];
            for (int col = 0; col < cols; col++) {
                int element = row * cols + col;
                aRow[col] = imageData[element];
            }

            /*
             * Now we want to move the data into the three dimensional
             * array.  We will use the bitwise AND and bitwise right 
             * operations to filter out all but the correct set of eight
             * bits we are looking for.
             */
            for (int col = 0; col < cols; col++) {
                /*
                 * Alpha Data
                 */
                data[row][col][0] = (aRow[col] >> 24) & 0xFF;

                /*
                 * Red data
                 */
                data[row][col][1] = (aRow[col] >> 16) & 0xFF;

                /*
                 * Green data
                 */
                data[row][col][2] = (aRow[col] >> 8) & 0xFF;

                /*
                 * Blue data
                 */
                data[row][col][3] = (aRow[col]) & 0xFF;
            }
        }

        return data;
    }

    /*
     * This method will take our easy to work with three dimensional array and
     * process it back into a one dimensional array so we can make it into 
     * an actual image.  This is exaclty the opposite process of the get3DArray
     * method.
     */
    private static int[] get1DArray(int[][][] imageData, int cols, int rows)
    {
        /*
         * This one dimensional array will be populated with the pixel information
         * in the three dimensional array.  This array will need to be large enough
         * to hold four numbers for each pixel in the image.
         */
        int[] data = new int[cols * rows * 4];

        /*
         * Now we can move the pixel data back into the one dimensional array.
         * we will use the bitwise OR operator and the left operator to put
         * the four eight bit bytes into each int in the array.
         */
        for (int row = 0, count = 0; row < rows; row++) {
            for (int col = 0;col < cols;col++) {
                data[count] = ((imageData[row][col][0] << 24) & 0xFF000000)
                               | ((imageData[row][col][1] << 16) & 0x00FF0000)
                               | ((imageData[row][col][2] << 8) & 0x0000FF00)
                               | ((imageData[row][col][3]) & 0x000000FF);
                count++;
            }
        }
        
        return data;
    }

    private static final int INSERTIONPOINT = 4096;

    private static int[][][] encodeMessage(int[][][] origData, int cols, int rows, String msg)
    {
        /*
         * First we will make a copy so we don't change the original
         * image.
         */
        int[][][] imgData = new int[rows][cols][4];
        for (int row = 0;row < rows;row++) {
            for (int col = 0;col < cols;col++) {
                imgData[row][col][0] = origData[row][col][0];
                imgData[row][col][1] = origData[row][col][1];
                imgData[row][col][2] = origData[row][col][2];
                imgData[row][col][3] = origData[row][col][3];
            }
        }

        /*
         * Each pixel has three two bit pairs of information, one for
         * the red value, one for the green value, and one for the
         * blue value.  We will store two bits ot information in each 
         * of these values.  The first step is to convert the String
         * into an array of bytes.  Java make space for two bytes 
         * for every character in a string.  This is enough space
         * to cover "wide" langauges like Japanese and Chinese.  
         * However, that also makes every string twice as long.  Our
         * sample will only support one byte for each character 
         * which is long enough for all the ASCII or Latin-1 
         * characters.  This makes the routine easier to handle and
         * makes our footprint on the image even smaller.  
         * 
         * We could employ various schemes to make the space we need
         * for each character even smaller, but that would reduce the
         * number of characters we could support.  Latin-1 is 
         * important because many encryption schemes represent 
         * encrypted values in Latin-1 characters.  By supporting
         * Latin-1 our program will support hiding encrypted data
         * from many encryption schemes.
         */
        
        byte[] msgBytes = null;
        try {
            msgBytes = msg.getBytes("ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            /*
             * Java makes us catch this exception, but all platforms
             * support ISO-8859-1 (which is code for Latin-1) so
             * this exception should never be thrown.
             */
            e.printStackTrace();
            return null;
        }
        

        /*
         * Each pixel can hold six bits of information.  However, 
         * each of our characters contains 8 bits (one byte) of 
         * information.  If we let the last byte run into the next
         * siz bit section we could have a pixel where we only 
         * changed the red value.  That would be a noticable change.
         * We need to make sure our string will fill up an even
         * number of RGB values.  8 bits - 6 = 2 remaining.  Add
         * those to our next value means 10 bits - 6 = 4 remaining.
         * Add those to our next value means 12 bits - 6 = 6 bits 
         * remaining.  That means the rest of the 8 bits from that
         * last character (6 bits) will fit into the next 6 bit
         * RGB value.  So... we need to make sure that the length
         * of our array of bytes is a multiple of three.
         * 
         * Instead of trying to track this later, it is easier to 
         * just pad our array out to a multiple of three.  However,
         * we need to take into account the fact that we are going
         * to add a terminator character on the end so we really 
         * need to pad it out until the length plus one is a multiple
         * of three.  Since the byte value of ! is our terminator
         * we can just pad with extra ! bytes and they will be ignored
         * when they are read on the other end.
         */
        if ((msgBytes.length + 1 % 3 != 0)) {
            int toAdd = (3 - (msgBytes.length % 3)) - 1;
            byte tmpBytes[] = new byte[msgBytes.length + toAdd];
            
            for (int i = 0; i < toAdd; i++) {
                tmpBytes[msgBytes.length + i] = (byte)'!';
            }

            System.arraycopy(msgBytes, 0, tmpBytes, 0, msgBytes.length);
            msgBytes = tmpBytes;
        }

        /*
         * We need to make sure we know our where our string starts
         * and stops.  We will make sure our string starts with the 
         * byte value of ~~~ and ends with the byte value of !.  This
         * is how we will be sure we found our string.
         */
        byte tmpBytes[] = new byte[msgBytes.length + 4];
        tmpBytes[0] = '~';
        tmpBytes[1] = '~';
        tmpBytes[2] = '~';
        tmpBytes[msgBytes.length + 3] = (byte)'!';
        System.arraycopy(msgBytes, 0, tmpBytes, 3, msgBytes.length);
        msgBytes = tmpBytes;
        
        /*
         * Now that we have the right number of bytes we need to split
         * them up into two bit chunks.  This will result in an array
         * which is four times longer than we started with.  This is
         * why we don't support unicode.  If we did the array would
         be 8 times longer.  
         */
        byte[] twoBitData = new byte[4 * msgBytes.length];

        /*
         * We want to break each 8 bit sequence into two bit pairs.  
         * This means we need to take first bits 1-2, then 3-4, then
         * 5-6, and then 7-8.  We do this by shifting first 0 then 2, 
         * then 4, then 6 places.  We will also mask the bits so they 
         * are value RGB values.
         */
        int twoBitCount = 0;
        for (byte element:msgBytes) {
            twoBitData[twoBitCount++] = (byte) (element & LSB_MASK_READ);
            twoBitData[twoBitCount++] = (byte) ((element >> 2) & LSB_MASK_READ);
            twoBitData[twoBitCount++] = (byte) ((element >> 4) & LSB_MASK_READ);
            twoBitData[twoBitCount++] = (byte) ((element >> 6) & LSB_MASK_READ);
        }

        /*
         * At this point we have finished encoding our string, but we
         * haven't added it to the image yet.  Now we will actually
         * change the pixels values and encode out message.
         * 
         * We will start adding our values at a predefined insertion
         * point.  We don't want to start right at the beginning
         * since that might be more noticeable.  We could make our
         * application even more secure by making the user specify
         * the insertion point, but this application is about
         * obscurity rather than security so we will just have a set
         * insertion point. 
         * 
         * We don't want to edit all of our pixels all in a row since
         * that would also be more noticeable.  We will use the last
         * value of the six bits as the skip value to make things a 
         * little more difficult to detect
         */
        int skipCount = 0;

        /*
         * Now we can actually encode our values.
         */
        twoBitCount = 0;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                /*
                 * We want to embed siz bits in each RGB value
                 * and then skip ahead based on the last two bits
                 * of that value.  We know that our byte array will
                 * work well with this scheme since we padded it
                 * out before we encoded our string.
                 */
                if ((row * col > INSERTIONPOINT) && 
                    (twoBitCount < twoBitData.length) && 
                    skipCount-- == 0) {
                    /*
                     * We want to replace the two least significant bits of
                     * red, green, and blue values with our value.  The least
                     * significant bits are the last bits of the byte and 
                     * replacing them will cause the smallest visual change
                     * in the color.
                     */
                    imgData[row][col][1] = (imgData[row][col][1] & LSB_MASK_WRITE) | twoBitData[twoBitCount++];
                    imgData[row][col][2] = (imgData[row][col][2] & LSB_MASK_WRITE) | twoBitData[twoBitCount++];
                    imgData[row][col][3] = (imgData[row][col][3] & LSB_MASK_WRITE) | twoBitData[twoBitCount++];

                    /*
                     * The last step is to see how many pixels we will skip 
                     * before changing the next pixel
                     */
                    skipCount = twoBitData[twoBitCount - 1];
                }
            }
        }

        return imgData;
    }

    /*
     * This value must be a multiple of both three and four so we
     * can parse each RGB value in threes and each byte value in 
     * fours and make sure we get a whole character each time.
     */
    private static final int BUFFER_LENGTH = 768;

    private static int m_startCharCount = 0;
    private static boolean m_foundTerminator = false;

    private static String getMessage(int[][][] data, int cols, int rows)
    {
        /**
         * This method is basically the opposite of the encodeMessage method.
         * This method will take an image with a message encoded in it and 
         * extract the message.  
         * 
         * We will calculate the skip count as we go along.  We know that the 
         * first one is always zero.  
         * 
         */
        int skipCount = 0;
        int twoBitCount = 0;
        m_startCharCount = 0;
        m_foundTerminator = false;
        
        /*
         * We don't know when the value might end, so we will create an array
         * large enough to hold the largest possible string this image could
         * hold.  We can't be sure if we have hit the end of the message until
         * we find the terminator character.  However, we can't figure out if we
         * have found the terminator character until we can reassemble the byte
         * value.  We don't want to parse the whole image because it can be very
         * slow for large images.  To avoid this problem we will parse a chunk
         * of the bytes, reassemble the characters so we can find the terminator,
         * and then parse some more.
         *
         * We need to initialize an array big enough to hold all of that data.
         * That will be three times as large as the image because each pixel
         * holds three two bit pairs of information.
         */
        byte[] twoBitData = new byte[(rows * cols - INSERTIONPOINT) * 3];
        int byteCount = 0;
        StringBuffer message = new StringBuffer();
        for (int row=0; row < rows; row++) {
            for (int col=0; col < cols; col++) {
                if ((row * col > INSERTIONPOINT) && (skipCount-- == 0)) {
                    /*
                     * Now we will read out the data two bits at a time.
                     */
                    twoBitData[twoBitCount++] = (byte) (data[row][col][1] & LSB_MASK_READ);
                    twoBitData[twoBitCount++] = (byte) (data[row][col][2] & LSB_MASK_READ);
                    twoBitData[twoBitCount++] = (byte) (data[row][col][3] & LSB_MASK_READ);

                    byteCount += 3;

                    /*
                     * We can figure out the skip count by looking at the 
                     * last value we read out of the encoded data.
                     */
                    skipCount = twoBitData[twoBitCount - 1];

                    if ((byteCount % BUFFER_LENGTH) == 0) {
                        message.append(decodeString(twoBitData, twoBitCount - BUFFER_LENGTH, twoBitCount));
                        if (m_foundTerminator) {
                            return message.toString();
                        }
                    }
                }
            }
        }

        return null;
    }

    private static String decodeString(byte[] twoBitData, int start, int end)
    {
        /*
         * At this point the array twoBitData should be filled with all of the 
         * two bit pairs in the image after the insertion point.  This will 
         * include the encoded message and also a lot of junk from all the pixels
         * after the encoded string.  We now need to create characters after all
         * the data until the terminator character.
         */
        StringBuffer message = new StringBuffer();
        int twoBitCount = start;

        /*
         * The data was stored in three bit pairs, but now we have extracted it so
         * we can we can read through it four two bit pairs (one byte) at a time.
         */
        for (int i = start; i < end; i += 4) {
            /*
             * Now we can shift and OR the pairs back to normal;
             */
            byte element = (byte) twoBitData[twoBitCount++];
            element = (byte) (element | (twoBitData[twoBitCount++] << 2));
            element = (byte) (element | (twoBitData[twoBitCount++] << 4));
            element = (byte) (element | (twoBitData[twoBitCount++] << 6));

            /*
             * We want to make sure we are reading a valid string so we look for
             * our special start sequence which is the byte value of ~~~.  This
             * lets us give a good error message if the user tries to get a message
             * out if an image that doesn't have one.
             */
            if (element == '~') {
                m_startCharCount++;
                continue;
            }
            
            /*
             * If we didn't find our starting sequence then there is no reason to 
             * read the rest of the data.
             */
            if (m_startCharCount < 3) {
                return null;
            }

            /*
             * Our terminator character is the byte value of !.  When we find that
             * character we know we are done.
             */
            if (element == '!') {
                m_foundTerminator = true;
                break;
            }

            /*
             * All other characters are part of the message.
             */
            message.append((char)(element));
        }

        return message.toString();
    }

}
