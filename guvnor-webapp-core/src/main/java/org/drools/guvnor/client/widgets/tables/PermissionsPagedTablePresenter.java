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

package org.drools.guvnor.client.widgets.tables;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.ui.*;
import org.drools.guvnor.client.asseteditor.MultiViewRow;
import org.drools.guvnor.client.common.FormStylePopup;
import org.drools.guvnor.client.common.GenericCallback;
import org.drools.guvnor.client.common.ImageButton;
import org.drools.guvnor.client.common.InfoPopup;
import org.drools.guvnor.client.common.LoadingPopup;
import org.drools.guvnor.client.common.RulePackageSelector;
import org.drools.guvnor.client.common.SmallLabel;
import org.drools.guvnor.client.messages.ConstantsCore;
import org.drools.guvnor.client.resources.GuvnorImages;
import org.drools.guvnor.client.resources.ImagesCore;
import org.drools.guvnor.client.rpc.*;
import org.drools.guvnor.client.widgets.categorynav.CategoryExplorerWidget;
import org.drools.guvnor.client.widgets.categorynav.CategorySelectHandler;
import org.drools.guvnor.client.widgets.query.OpenItemCommand;
import org.drools.guvnor.client.widgets.tables.PermissionsPagedTableView.Presenter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import org.drools.guvnor.shared.security.AppRoles;


public class PermissionsPagedTablePresenter implements Presenter {
    protected RepositoryServiceAsync repositoryService = GWT.create(RepositoryService.class);
    private final PermissionsPagedTableView view;
   
    public PermissionsPagedTablePresenter(PermissionsPagedTableView view) {
        this.view = view;
        bind();
    }
    
    public void bind() {
        Command newUserCommand = new Command() {
            public void execute() {
                Image image = new Image(ImagesCore.INSTANCE.snapshot());
                image.setAltText(ConstantsCore.INSTANCE.Snapshot());
                final FormStylePopup form = new FormStylePopup(image,
                                                                ConstantsCore.INSTANCE.EnterNewUserName() );
                final TextBox userName = new TextBox();
                form.addAttribute( ConstantsCore.INSTANCE.NewUserName(),
                                   userName );

                Button btnOK = new Button( ConstantsCore.INSTANCE.OK() );
                form.addAttribute( "",
                                   btnOK );
                btnOK.addClickHandler( new ClickHandler() {

                    public void onClick(ClickEvent event) {
                        if ( userName.getText() != null
                             && userName.getText().length() != 0 ) {
                            repositoryService.createUser( userName.getText(),
                                                                              new GenericCallback<java.lang.Void>() {
                                                                                  public void onSuccess(Void a) {
                                                                                      view.refresh();
                                                                                      showEditor( userName.getText() );
                                                                                  }

                                                                                  public void onFailure(Throwable t) {
                                                                                      super.onFailure( t );
                                                                                  }
                                                                              } );
                            form.hide();
                        }
                    }
                } );
                form.show();
            }
        };
        view.setNewUserCommand(newUserCommand);

        Command deleteUserCommand = new Command() {
            public void execute() {
                final String userName = view.getSelectionModel().getSelectedObject().getUserName();
                if ( userName != null
                        && Window.confirm(ConstantsCore.INSTANCE.AreYouSureYouWantToDeleteUser0(userName)) ) {
                    repositoryService.deleteUser( userName,
                                                                         new GenericCallback<java.lang.Void>() {
                                                                             public void onSuccess(Void a) {
                                                                                 view.refresh();
                                                                             }
                                                                         } );
                }
            }
        };
        view.setDeleteUserCommand(deleteUserCommand);
        
        OpenItemCommand openSelectedCommand = new OpenItemCommand() {
            public void open(String key) {
                showEditor( key );
            }

            public void open(MultiViewRow[] rows) {
                // Not implemented
            }
        };
        view.setOpenSelectedCommand(openSelectedCommand);
        
        
        AsyncDataProvider<PermissionsPageRow> dataProvider = new AsyncDataProvider<PermissionsPageRow>() {
            protected void onRangeChanged(HasData<PermissionsPageRow> display) {
                PageRequest request = new PageRequest();
                request.setStartRowIndex( view.pager.getPageStart() );
                request.setPageSize( view.pageSize );
                repositoryService.listUserPermissions( request,
                                                             new GenericCallback<PageResponse<PermissionsPageRow>>() {
                                                                 public void onSuccess(PageResponse<PermissionsPageRow> response) {
                                                                     updateRowCount( response.getTotalRowSize(),
                                                                                     response.isTotalRowSizeExact() );
                                                                     updateRowData( response.getStartRowIndex(),
                                                                                    response.getPageRowList() );
                                                                 }
                                                             } );
            }
        };
        view.setDataProvider(dataProvider);       
    }

    private void showEditor(final String userName) {
        LoadingPopup.showMessage(ConstantsCore.INSTANCE.LoadingUsersPermissions());
        repositoryService.retrieveUserPermissions( userName,
                                                                       new GenericCallback<Map<String, List<String>>>() {
                                                                           public void onSuccess(final Map<String, List<String>> perms) {
                                                                               doPermissionEditor(userName ,perms);
                                                                               LoadingPopup.close();
                                                                           }

                                                                       } );
    }
    
    private void doPermissionEditor(final String userName, final Map<String, List<String>> perms) {
        Image image = new Image(ImagesCore.INSTANCE.management());
        image.setAltText(ConstantsCore.INSTANCE.Management());
        final FormStylePopup editor = new FormStylePopup(image,
                ConstantsCore.INSTANCE.EditUser0(userName));
        editor.addRow(new HTML("<i>" + ConstantsCore.INSTANCE.UserAuthenticationTip()
                + "</i>"));
        // now render the actual permissions...
        VerticalPanel vp = new VerticalPanel();
        editor.addAttribute("", doPermsPanel(perms, vp));

        HorizontalPanel hp = new HorizontalPanel();
        Button save = new Button(ConstantsCore.INSTANCE.SaveChanges());
        hp.add(save);
        editor.addAttribute("", hp);
        save.addClickHandler(createClickHandlerForSaveButton(userName, perms,
                editor));

        Button cancel = new Button(ConstantsCore.INSTANCE.Cancel());
        hp.add(cancel);
        cancel.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent w) {
                editor.hide();
            }
        });
        editor.show();
    }

    private ClickHandler createClickHandlerForSaveButton(final String userName,
            final Map<String, List<String>> perms, final FormStylePopup editor) {
        return new ClickHandler() {
            public void onClick(ClickEvent w) {
                LoadingPopup.showMessage(ConstantsCore.INSTANCE.Updating());
                repositoryService.updateUserPermissions(
                        userName, perms, new GenericCallback<java.lang.Void>() {
                            public void onSuccess(Void a) {
                                LoadingPopup.close();
                                view.refresh();
                                editor.hide();
                            }
                        });
            }
        };
    }

    /**
     * The permissions panel.
     */
    private Widget doPermsPanel(final Map<String, List<String>> perms,
                                final Panel vp) {
        vp.clear();

        for ( Map.Entry<String, List<String>> perm : perms.entrySet() ) {
            if ( perm.getKey().equals( "admin" ) ) { // NON-NLS
                HorizontalPanel h = new HorizontalPanel();
                h.add( new HTML( "<b>"
                                 + ConstantsCore.INSTANCE.ThisUserIsAnAdministrator()
                                 + "</b>" ) ); // NON-NLS
                Button del = new Button( ConstantsCore.INSTANCE.RemoveAdminRights() );

                del.addClickHandler( new ClickHandler() {
                    public void onClick(ClickEvent w) {
                        if ( Window.confirm( ConstantsCore.INSTANCE.AreYouSureYouWantToRemoveAdministratorPermissions() ) ) {
                            perms.remove( "admin" ); // NON-NLS
                            doPermsPanel( perms,
                                          vp );
                        }
                    }
                } );
                h.add( del );
                vp.add( h );
            } else {
                final String permType = perm.getKey();
                final List<String> permList = perm.getValue();

                Grid g = new Grid( permList.size() + 1,
                                   3 );
                g.setWidget( 0,
                             0,
                             new HTML( "<b>["
                                       + permType
                                       + "] for:</b>" ) ); // NON-NLS

                for ( int i = 0; i < permList.size(); i++ ) {
                    final String p = permList.get( i );
                    ImageButton del = new ImageButton( GuvnorImages.INSTANCE.DeleteItemSmall(),
                                                       ConstantsCore.INSTANCE.RemovePermission(),
                                                       new ClickHandler() {
                                                           public void onClick(ClickEvent w) {
                                                               if ( Window.confirm( ConstantsCore.INSTANCE.AreYouSureYouWantToRemovePermission0( p ) ) ) {
                                                                   permList.remove( p );
                                                                   if ( permList.size() == 0 ) {
                                                                       perms.remove( permType );
                                                                   }
                                                                   doPermsPanel( perms,
                                                                                 vp );
                                                               }
                                                           }
                                                       } );

                    g.setWidget( i + 1,
                                 1,
                                 new SmallLabel( p ) );
                    g.setWidget( i + 1,
                                 2,
                                 del );
                }

                vp.add( g );
            }

        }

        // now to be able to add...
        ImageButton newPermission = new ImageButton( GuvnorImages.INSTANCE.NewItem(),
                                                     ConstantsCore.INSTANCE.AddANewPermission(),
                                                     createClickHandlerForNewPersmissionImageButton( perms,
                                                                                                     vp ) );
        vp.add( newPermission );
        return vp;
    }

    private ClickHandler createClickHandlerForNewPersmissionImageButton(final Map<String, List<String>> perms,
                                                                        final Panel vp) {
        return new ClickHandler() {
            public void onClick(ClickEvent w) {
                final FormStylePopup pop = new FormStylePopup();
                final ListBox permTypeBox = new ListBox();
                permTypeBox.addItem( ConstantsCore.INSTANCE.Loading() );

                HorizontalPanel hp = new HorizontalPanel();
                hp.add( permTypeBox );
                hp.add( new InfoPopup( ConstantsCore.INSTANCE.PermissionDetails(),
                                       ConstantsCore.INSTANCE.PermissionDetailsTip() ) );
                pop.addAttribute( ConstantsCore.INSTANCE.PermissionType(),
                                  hp );


                permTypeBox.clear();
                permTypeBox.addItem(ConstantsCore.INSTANCE.pleaseChoose1());
                for (AppRoles role : AppRoles.values()) {
                    permTypeBox.addItem(role.getName());
                }

                permTypeBox.addChangeHandler( createChangeHandlerForPermTypeBox( perms,
                                                                                 vp,
                                                                                 pop,
                                                                                 permTypeBox ) );

                pop.show();
            }

            private ChangeHandler createChangeHandlerForPermTypeBox(final Map<String, List<String>> perms,
                                                                    final Panel vp,
                                                                    final FormStylePopup pop,
                                                                    final ListBox permTypeBox) {
                return new ChangeHandler() {
                    public void onChange(ChangeEvent event) {
                        pop.clear();
                        final String sel = permTypeBox.getItemText( permTypeBox.getSelectedIndex() );
                        if ( sel.equals( "admin" ) ) { // NON-NLS
                            createButtonsAndHandlersForAdmin( perms,
                                                              vp,
                                                              pop );
                        } else if ( sel.startsWith( "analyst" ) ) { // NON-NLS
                            CategoryExplorerWidget cat = createCategoryExplorerWidget( perms,
                                                                                       vp,
                                                                                       pop,
                                                                                       sel );
                            pop.addAttribute( ConstantsCore.INSTANCE.SelectCategoryToProvidePermissionFor(),
                                              cat );
                        } else if ( sel.startsWith( "package" ) ) {
                            createButtonsPanelsAndHandlersForPackage( perms,
                                                                      vp,
                                                                      pop,
                                                                      sel );
                        }
                    }

                    private void createButtonsPanelsAndHandlersForPackage(final Map<String, List<String>> perms,
                                                                          final Panel vp,
                                                                          final FormStylePopup pop,
                                                                          final String sel) {
                        final RulePackageSelector rps = new RulePackageSelector( true );
                        Button ok = new Button( ConstantsCore.INSTANCE.OK() );
                        ok.addClickHandler( new ClickHandler() {
                            public void onClick(ClickEvent w) {
                                String pkName = rps.getSelectedPackage();
                                if ( perms.containsKey( sel ) ) {
                                    perms.get( sel ).add( "package="
                                                          + pkName ); // NON-NLS
                                } else {
                                    List<String> ls = new ArrayList<String>();
                                    ls.add( "package="
                                            + pkName ); // NON-NLS
                                    perms.put( sel,
                                               ls );
                                }

                                doPermsPanel( perms,
                                              vp );
                                pop.hide();

                            }
                        } );

                        HorizontalPanel hp = new HorizontalPanel();
                        hp.add( rps );
                        hp.add( ok );
                        pop.addAttribute( ConstantsCore.INSTANCE.SelectPackageToApplyPermissionTo(),
                                          hp );
                    }

                    private CategoryExplorerWidget createCategoryExplorerWidget(final Map<String, List<String>> perms,
                                                                                final Panel vp,
                                                                                final FormStylePopup pop,
                                                                                final String sel) {
                        return new CategoryExplorerWidget( new CategorySelectHandler() {
                            public void selected(String selectedPath) {
                                if ( perms.containsKey( sel ) ) {
                                    perms.get( sel ).add( "category="
                                                          + selectedPath ); // NON-NLS
                                } else {
                                    List<String> ls = new ArrayList<String>();
                                    ls.add( "category="
                                            + selectedPath ); // NON-NLS
                                    perms.put( sel,
                                               ls );
                                }
                                doPermsPanel( perms,
                                              vp );
                                pop.hide();
                            }
                        } );
                    }

                    private void createButtonsAndHandlersForAdmin(final Map<String, List<String>> perms,
                                                                  final Panel vp,
                                                                  final FormStylePopup pop) {
                        Button ok = new Button( ConstantsCore.INSTANCE.OK() );

                        pop.addAttribute( ConstantsCore.INSTANCE.MakeThisUserAdmin(),
                                          ok );
                        ok.addClickHandler( new ClickHandler() {
                            public void onClick(ClickEvent w) {
                                perms.put( "admin",
                                           new ArrayList<String>() ); // NON-NLS

                                doPermsPanel( perms,
                                              vp );
                                pop.hide();
                            }
                        } );
                        Button cancel = new Button( ConstantsCore.INSTANCE.Cancel() );

                        pop.addAttribute( "",
                                          cancel );
                        cancel.addClickHandler( new ClickHandler() {
                            public void onClick(ClickEvent w) {
                                pop.hide();
                            }
                        } );
                    }
                };
            }
        };
    }

}
