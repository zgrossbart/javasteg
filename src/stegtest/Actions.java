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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

/*
 * This class handles the actions in the menu bar.  These action don't
 * do anything themselves, they just call back to the Main class.
 */
public class Actions
{
	protected void initMenus(JFrame frame)
	{
        JMenuBar mb = new JMenuBar();
		
		frame.setJMenuBar(mb);
		
		JMenu file = new JMenu("File");
        mb.add(file);

        file.add(new OpenImageAction());
		file.add(new SaveImageAction());
        file.add(new GetMessageAction());
	}
}

/*
 * This action calls the open method to open the object to add the 
 * message to.  
 */
class OpenImageAction extends AbstractAction
{
	public void actionPerformed(ActionEvent e)
	{
		StegTest.getStegTest().openImage();
	}
	public boolean isEnabled()
	{
		return true;
	}
	
	public Object getValue(String key)
	{
        if(key.equals("MnemonicKey"))
			return (int) 'O';
		else if(key.equals("Name"))
			return "Open Image";
		else if(key.equals("ShortDescription"))
			return "Open Image";
		else if(key.equals("SmallIcon"))
			return null;
		else if(key.equals("ActionCommandKey"))
			return null;
		else if(key.equals("AcceleratorKey"))
			return KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_MASK);
		else
            return super.getValue(key);
	}
}

/*
 * This actions calls saveImage to  save the image with the encoded
 * message.
 */
class SaveImageAction extends AbstractAction
{
	public void actionPerformed(ActionEvent e)
	{
		StegTest.getStegTest().saveImage();
	}
	public boolean isEnabled()
	{
		return true;
	}
	
	public Object getValue(String key)
	{
        if(key.equals("MnemonicKey"))
			return (int) 'S';
		else if(key.equals("Name"))
			return "Save Image";
		else if(key.equals("ShortDescription"))
			return "Save Image";
		else if(key.equals("SmallIcon"))
			return null;
		else if(key.equals("ActionCommandKey"))
			return null;
		else if(key.equals("AcceleratorKey"))
			return KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_MASK);
		else
            return super.getValue(key);
	}
}

/*
 * This action calls the getMessage method to get the encoded message out 
 * of an image.
 */
class GetMessageAction extends AbstractAction
{
	public void actionPerformed(ActionEvent e)
	{
		StegTest.getStegTest().getMessage();
	}
	public boolean isEnabled()
	{
		return true;
	}
	
	public Object getValue(String key)
	{
        if(key.equals("MnemonicKey"))
			return (int) 'G';
		else if(key.equals("Name"))
			return "Get Message";
		else if(key.equals("ShortDescription"))
			return "Get Message";
		else if(key.equals("SmallIcon"))
			return null;
		else if(key.equals("ActionCommandKey"))
			return null;
		else if(key.equals("AcceleratorKey"))
			return KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.CTRL_MASK);
		else
            return super.getValue(key);
	}
}
