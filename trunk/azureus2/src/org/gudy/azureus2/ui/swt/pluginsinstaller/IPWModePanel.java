/*
 * Created on 29 nov. 2004
 * Created by Olivier Chalouhi
 * 
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 * AELITIS, SARL au capital de 30,000 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.pluginsinstaller;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.wizard.AbstractWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.IWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.Wizard;

/**
 * @author Olivier Chalouhi
 *
 */
public class IPWModePanel extends AbstractWizardPanel {
  
  private static final int MODE_FROM_LIST = 0;
  private static final int MODE_FROM_FILE = 1;
  private static final int MODE_FROM_URL  = 2;
  
  private int mode = MODE_FROM_LIST;
  
  public 
  IPWModePanel(
	Wizard 					wizard, 
	IWizardPanel 			previous ) 
  {
	super(wizard, previous);
  }


  public void 
  show() 
  {
	wizard.setTitle(MessageText.getString("installPluginsWizard.mode.title"));
	
	Composite rootPanel = wizard.getPanel();
	GridLayout layout = new GridLayout();
	layout.numColumns = 1;
	rootPanel.setLayout(layout);

	Composite panel = new Composite(rootPanel, SWT.NULL);
	GridData gridData = new GridData(GridData.VERTICAL_ALIGN_CENTER | GridData.FILL_HORIZONTAL);
	panel.setLayoutData(gridData);
	layout = new GridLayout();
	layout.numColumns = 1;
	panel.setLayout(layout);

	Button bListMode = new Button(panel,SWT.RADIO);
	Messages.setLanguageText(bListMode,"installPluginsWizard.mode.list");
	bListMode.setData("mode",new Integer(MODE_FROM_LIST));
	GridData data = new GridData(GridData.FILL_VERTICAL);
	data.verticalAlignment = GridData.VERTICAL_ALIGN_END;
	bListMode.setLayoutData(data);
	
	
	Button bFileMode = new Button(panel,SWT.RADIO);
	Messages.setLanguageText(bFileMode,"installPluginsWizard.mode.file");
	bFileMode.setData("mode",new Integer(MODE_FROM_FILE));
	data = new GridData(GridData.FILL_VERTICAL);
	data.verticalAlignment = GridData.VERTICAL_ALIGN_BEGINNING;
	bFileMode.setLayoutData(data);
	
	
	Listener modeListener = new Listener() {
	  public void handleEvent(Event e) {
	    mode = ((Integer) e.widget.getData("mode")).intValue();
	  }
	};

	bListMode.addListener(SWT.Selection,modeListener);
}

	public IWizardPanel 
	getNextPanel()
	{
	  switch(mode) {
	    case MODE_FROM_LIST :
	      return new IPWListPanel(wizard,this);
	    
	    case MODE_FROM_FILE :
	      return new IPWFilePanel(wizard,this);
	  }

	  return null;
	}
	
	public boolean 
	isNextEnabled() 
	{
	   return true;
	}
	
	public boolean 
	isFinishEnabled() 
	{
	   return( false );
	}
}
