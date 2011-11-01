/*
 * Copyright 2011 JBoss Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.drools.guvnor.client.widgets.drools.workitems;

import org.drools.ide.common.shared.workitems.PortableBooleanParameterDefinition;
import org.drools.ide.common.shared.workitems.PortableEnumParameterDefinition;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * A Widget to display a Work Item Boolean parameter
 */
public class WorkItemBooleanParameterWidget extends WorkItemParameterWidget {

    interface WorkItemBooleanParameterWidgetBinder
        extends
        UiBinder<HorizontalPanel, WorkItemBooleanParameterWidget> {
    }

    @UiField
    Label                                               parameterName;

    @UiField
    ListBox                                             parameterValues;

    @UiField
    ListBox                                             lstAvailableBindings;

    private static WorkItemBooleanParameterWidgetBinder uiBinder = GWT.create( WorkItemBooleanParameterWidgetBinder.class );

    public WorkItemBooleanParameterWidget(PortableBooleanParameterDefinition ppd) {
        super( ppd );
        this.parameterName.setText( ppd.getName() );
        boolean isItemSelected = false;
        Boolean selectedItem = ppd.getValue();
        if ( ppd.getValues() != null ) {
            for ( int index = 0; index < ppd.getValues().length; index++ ) {
                Boolean item = ppd.getValues()[index];
                this.parameterValues.addItem( Boolean.toString( item ) );
                if ( item.equals( selectedItem ) ) {
                    this.parameterValues.setSelectedIndex( index );
                    isItemSelected = true;
                }
            }
            if ( !isItemSelected ) {
                this.parameterValues.setSelectedIndex( 0 );
                ppd.setValue( Boolean.valueOf( this.parameterValues.getItemText( 0 ) ) );
            }
        }
    }

    @Override
    protected Widget getWidget() {
        return uiBinder.createAndBindUi( this );
    }

    @UiHandler("parameterValues")
    void parameterValuesOnChange(ChangeEvent event) {
        int index = this.parameterValues.getSelectedIndex();
        if ( index == -1 ) {
            ((PortableBooleanParameterDefinition) ppd).setValue( null );
        } else {
            ((PortableBooleanParameterDefinition) ppd).setValue( Boolean.valueOf( this.parameterValues.getItemText( index ) ) );
        }
    }

}