A [Steganography](http://en.wikipedia.org/wiki/Steganography) GUI for Java
==================================================

This little Java program hides secret messages in images by making subtle changes to specific pixels you can't see like this:

![Pixels](/zgrossbart/javasteg/raw/master/pixels.gif)

These changes come all together in the image to encode the message you have.

![Encoded pixels highlighted](/zgrossbart/javasteg/raw/master/blue_pix.jpg)

Find out more and run this code online at [How To Keep a Secret Secret](http://www.zackgrossbart.com/hackito/secret-secret).


Building and Running javasteg
--------------------------------------

Javasteg is built with Apache Ant.  Once ant is installed just go 
to the java steg directory and run the ant command:

<pre><code>    ant
</code></pre>
        
Once the application is built you can run it with this command:

<pre><code>    java -jar dist/stegtest.jar lily.jpg
</code></pre>
    
Pass in any other images you would like.  The program supports GIF,
JPG, and PNG.
