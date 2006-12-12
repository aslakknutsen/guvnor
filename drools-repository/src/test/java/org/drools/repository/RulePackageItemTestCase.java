package org.drools.repository;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

public class RulePackageItemTestCase extends TestCase {

    public void testListPackages() throws Exception {
        RulesRepository repo = getRepo();
        repo.createRulePackage( "testListPackages1", "lalalala" );
        
        List list = iteratorToList( repo.listPackages() );
        int prevSize = list.size();
        repo.createRulePackage( "testListPackages2", "abc" );
        
        list = iteratorToList( repo.listPackages() );
        
        assertEquals(prevSize + 1, list.size());
    }
    
    public void testRulePackageItem() throws Exception {
        RulesRepository repo = getRepo();
        try {
            
            //calls constructor
            PackageItem rulePackageItem1 = repo.createRulePackage("testRulePackage", "desc");
            assertNotNull(rulePackageItem1);
            assertEquals("testRulePackage", rulePackageItem1.getName());
            
            Iterator it = getRepo().listPackages();
            assertTrue(it.hasNext());

            while (it.hasNext()) {
                PackageItem pack = (PackageItem) it.next();
                if (pack.getName().equals( "testRulePackage" )) {
                    return;
                }
            }
            fail("should have picked up the testRulePackage but didnt.");
        }
        catch(Exception e) {
            e.printStackTrace();
            fail("Caught unexpected exception: " + e);
        }
        
        //try constructing with node of wrong type
        try {            
            DslItem dslItem = repo.addDsl("testRulePackageItem", "content");
            PackageItem rulePackageItem2 = new PackageItem(repo, dslItem.getNode());
            fail("Exception not thrown for node of type: " + dslItem.getNode().getPrimaryNodeType().getName());
        }
        catch(RulesRepositoryException e) {
            //this is good
        }
        catch(Exception e) {
            e.printStackTrace();
            fail("Caught unexpected exception: " + e);
        }
    }
    
    /**
     * This tests creating a "baseline" of a RulePackage,
     * basically updating all the resources, and checking it in as a version.
     * This is showing off "multi dimensional versioning".
     */
    public void testBaselinePackage() throws Exception {
        RulesRepository repo = getRepo();
        
        PackageItem pack = repo.createRulePackage( "testBaselinePackage", "for testing baselines" );
        
        AssetItem rule1 = pack.addAsset( "rule 1", "yeah" );
        AssetItem rule2 = pack.addAsset( "rule 2", "foobar" );
        
        StateItem state = repo.getState( "deployed" );
        
        repo.save();
        
        assertNull(pack.getPrecedingVersion());
        
        //the first version, frozen with 2 rules
        pack.createBaseline("commit comment", state);
        
        //check head
        pack = repo.loadRulePackage( "testBaselinePackage" );
        assertEquals(2, iteratorToList(pack.getRules()).size());
        
        //now remove a rule from head
        pack.removeRule( "rule 1" );
        repo.save();
        assertEquals(1, iteratorToList( pack.getRules() ).size());
        
        pack.createBaseline( "another", state );
        
        PackageItem prev = (PackageItem) pack.getPrecedingVersion();
        assertEquals(2, iteratorToList( prev.getRules() ).size());
        
    }

    /** Continues to show how multi dimensional versioning works */
    public void testPackageBaselineWithRuleChanges() throws Exception {
        String packName = StackUtil.getCurrentMethodName();
        PackageItem pack = getRepo().createRulePackage( packName, "yeah" );
        
        AssetItem rule = pack.addAsset( "foobar", "waah" );        
        rule.updateContent( "this is something" );        
        rule.checkin( "something" );
        
        StateItem state = getRepo().getState( "something" );
        
        pack.createBaseline( "another one", state );
        
        pack = getRepo().loadRulePackage( packName );
        
        rule = (AssetItem) pack.getRules().next();
        rule.updateContent( "blah" );
        rule.checkin( "woot" );
        
        pack.createBaseline( "yeah", state );
        
        pack = getRepo().loadRulePackage( packName );
        rule = (AssetItem) pack.getRules().next();
        assertEquals("blah", rule.getContent());
        
        PackageItem prev = (PackageItem) pack.getPrecedingVersion();
        rule = (AssetItem) prev.getRules().next();
        assertEquals("this is something", rule.getContent());
        
    }

    private RulesRepository getRepo() {
        return RepositorySession.getRepository();
    }

    public void testLoadRulePackageItem() {

        PackageItem rulePackageItem = getRepo().createRulePackage("testLoadRuleRuleItem", "desc");

        rulePackageItem = getRepo().loadRulePackage("testLoadRuleRuleItem");
        assertNotNull(rulePackageItem);
        assertEquals("testLoadRuleRuleItem", rulePackageItem.getName());
        
        assertEquals("desc", rulePackageItem.getDescription());
        // try loading rule package that was not created 
        try {
            rulePackageItem = getRepo().loadRulePackage("anotherRuleRuleItem");
            fail("Exception not thrown loading rule package that was not created.");
        } catch (RulesRepositoryException e) {
            // that is OK!
            assertNotNull(e.getMessage());
        }
    }    
    
    /**
     * This will test getting rules of specific versions out of a package.
     */
    public void testPackageRuleVersionExtraction() throws Exception {
        PackageItem pack = getRepo().createRulePackage( "package extractor", "foo" );
        
        
        AssetItem rule1 = pack.addAsset( "rule number 1", "yeah man" );
        rule1.checkin( "version0" );
        
        AssetItem rule2 = pack.addAsset( "rule number 2", "no way" );
        rule2.checkin( "version0" );
        
        AssetItem rule3 = pack.addAsset( "rule number 3", "yes way" );
        rule3.checkin( "version0" );
        
        getRepo().save();
        
        pack = getRepo().loadRulePackage( "package extractor" );
        List rules = iteratorToList( pack.getRules() );
        assertEquals(3, rules.size());
        
        StateItem state = getRepo().getState( "foobar" );
        
        rule1.updateState( "foobar" );
        rule1.checkin( "yeah" );
        
        pack = getRepo().loadRulePackage( "package extractor" );
        
        rules = iteratorToList( pack.getRules(state) );
        
        assertEquals(1, rules.size());
        
        //now lets try an invalid state tag
        rules = iteratorToList( pack.getRules( getRepo().getState( "whee" ) ) );
        assertEquals(0, rules.size());
        
        //and null, as we start with null, should be able to get all three back
        //although an older version of one of them
        rules = iteratorToList( pack.getRules(null) );
        assertEquals(3, rules.size());
        
        //now do an update, and pull it out via state
        rule1.updateContent( "new content" );
        rule1.updateState( "draft" );
        rule1.checkin( "latest" );
        
        rules = iteratorToList( pack.getRules(getRepo().getState( "draft" )) );
        assertEquals(1, rules.size());
        AssetItem rule = (AssetItem) rules.get( 0 );
        assertEquals("new content", rule.getContent());
        
        //get the previous one via state
        
        rules = iteratorToList( pack.getRules(getRepo().getState( "foobar" )) );
        assertEquals(1, rules.size());
        AssetItem prior = (AssetItem) rules.get( 0 );
        
        assertFalse("new content".equals( prior.getContent() ));
        
    }
    
    public void testDuplicatePackageName() throws Exception {
        PackageItem pack = getRepo().createRulePackage( "dupePackageTest", "testing for dupe" );        
        assertNotNull(pack.getName());
        
        try {
            getRepo().createRulePackage( "dupePackageTest", "this should fail" );
            fail("Should not be able to add a package of the same name.");
        } catch (RulesRepositoryException e) {
            assertNotNull(e.getMessage());
        }
        
    }
    
    public void testLoadRulePackageItemByUUID() throws Exception {

        PackageItem rulePackageItem = getRepo().createRulePackage("testLoadRuleRuleItemByUUID", "desc");

        String uuid = null;
            uuid = rulePackageItem.getNode().getUUID();


        rulePackageItem = getRepo().loadRulePackageByUUID(uuid);
        assertNotNull(rulePackageItem);
        assertEquals("testLoadRuleRuleItemByUUID", rulePackageItem.getName());
        
        // try loading rule package that was not created 
        try {
            rulePackageItem = getRepo().loadRulePackageByUUID("01010101-0101-0101-0101-010101010101");
            fail("Exception not thrown loading rule package that was not created.");
        } catch (RulesRepositoryException e) {
            // that is OK!
            assertNotNull(e.getMessage());
        }
    }    
    
    public void testAddRuleRuleItem() {
            PackageItem rulePackageItem1 = getRepo().createRulePackage("testAddRuleRuleItem","desc");

            
            AssetItem ruleItem1 = rulePackageItem1.addAsset("testAddRuleRuleItem", "test description");
            ruleItem1.updateContent( "test content" );
            ruleItem1.checkin( "updated the rule content" );
            
            Iterator rulesIt = rulePackageItem1.getRules();
            assertNotNull(rulesIt);
            AssetItem first = (AssetItem) rulesIt.next();
            assertFalse(rulesIt.hasNext());
            assertEquals("testAddRuleRuleItem", first.getName());
            
            //test that it is following the head revision                        
            ruleItem1.updateContent("new lhs");
            ruleItem1.checkin( "updated again" );
            rulesIt = rulePackageItem1.getRules();
            assertNotNull(rulesIt);
            
            List rules = iteratorToList( rulesIt );
            assertEquals(1, rules.size());
            assertEquals("testAddRuleRuleItem", ((AssetItem)rules.get(0)).getName());
            assertEquals("new lhs", ((AssetItem)rules.get(0)).getContent());
                        
            AssetItem ruleItem2 = rulePackageItem1.addAsset("testAddRuleRuleItem2", "test content");
            
            rules = iteratorToList(rulePackageItem1.getRules());
            assertNotNull(rules);
            assertEquals(2, rules.size());  

    }

    
    List iteratorToList(Iterator it) {
        List list = new ArrayList();
        while(it.hasNext()) {
            list.add( it.next() );
        }
        return list;
    }

    public void testAddFunctionFunctionItem() {
            PackageItem rulePackageItem1 = getRepo().createRulePackage("testAddFunctionFunctionItem", "desc");
            
            FunctionItem functionItem1 = getRepo().addFunction("test function", "test content");
            
            rulePackageItem1.addFunction(functionItem1);
            
            List functions = rulePackageItem1.getFunctions();
            assertNotNull(functions);
            assertEquals(1, functions.size());
            assertEquals("test function", ((FunctionItem)functions.get(0)).getName());
            
            //test that it is following the head revision                        
            functionItem1.updateContent("new content");
            functions = rulePackageItem1.getFunctions();
            assertNotNull(functions);
            assertEquals(1, functions.size());
            assertEquals("test function", ((FunctionItem)functions.get(0)).getName());
            assertEquals("new content", ((FunctionItem)functions.get(0)).getContent());
                        
            FunctionItem functionItem2 = getRepo().addFunction("test function 2", "test content");
            rulePackageItem1.addFunction(functionItem2);
            
            functions = rulePackageItem1.getFunctions();
            assertNotNull(functions);
            assertEquals(2, functions.size());                          

    }

    public void testAddFunctionFunctionItemBoolean() {
            PackageItem rulePackageItem1 = getRepo().createRulePackage("testAddFunctionFunctionItemBoolean", "desc");
            
            FunctionItem functionItem1 = getRepo().addFunction("testAddFunctionFunctionItemBoolean", "test content");
            
            rulePackageItem1.addFunction(functionItem1, true);
            
            List functions = rulePackageItem1.getFunctions();
            assertNotNull(functions);
            assertEquals(1, functions.size());
            assertEquals("testAddFunctionFunctionItemBoolean", ((FunctionItem)functions.get(0)).getName());
            
            //test that it is following the head revision                        
            functionItem1.updateContent("new content");
            functions = rulePackageItem1.getFunctions();
            assertNotNull(functions);
            assertEquals(1, functions.size());
            assertEquals("testAddFunctionFunctionItemBoolean", ((FunctionItem)functions.get(0)).getName());
            assertEquals("new content", ((FunctionItem)functions.get(0)).getContent());
            
            FunctionItem functionItem2 = getRepo().addFunction("testAddFunctionFunctionItemBoolean2", "test content");
            rulePackageItem1.addFunction(functionItem2);
            
            functions = rulePackageItem1.getFunctions();
            assertNotNull(functions);
            assertEquals(2, functions.size());
            

    }

    public void testGetFunctions() {
            PackageItem rulePackageItem1 = getRepo().createRulePackage("testGetFunctions", "desc");
                        
            FunctionItem functionItem1 = getRepo().addFunction("testGetFunctions", "test content");
            
            rulePackageItem1.addFunction(functionItem1);
            
            List functions = rulePackageItem1.getFunctions();
            assertNotNull(functions);
            assertEquals(1, functions.size());
            assertEquals("testGetFunctions", ((FunctionItem)functions.get(0)).getName());
                                  
            FunctionItem functionItem2 = getRepo().addFunction("testGetFunctions2", "test content");
            rulePackageItem1.addFunction(functionItem2);
            
            functions = rulePackageItem1.getFunctions();
            assertNotNull(functions);
            assertEquals(2, functions.size());            

    }
    
    public void testGetRules() {
            PackageItem rulePackageItem1 = getRepo().createRulePackage("testGetRules", "desc");
                        
            AssetItem ruleItem1 = rulePackageItem1.addAsset("testGetRules", "desc" );
            ruleItem1.updateContent( "test lhs content" );

            
            List rules = iteratorToList(rulePackageItem1.getRules());
            assertNotNull(rules);
            assertEquals(1, rules.size());
            assertEquals("testGetRules", ((AssetItem)rules.get(0)).getName());
                                  
            AssetItem ruleItem2 = rulePackageItem1.addAsset("testGetRules2", "desc" );
            ruleItem2.updateContent( "test lhs content" );
            
            rules = iteratorToList(rulePackageItem1.getRules());
            assertNotNull(rules);
            assertEquals(2, rules.size());            

            //now lets test loading rule
            AssetItem loaded = rulePackageItem1.loadRule( "testGetRules" );
            assertNotNull(loaded);
            assertEquals("testGetRules", loaded.getName());
            assertEquals("desc", loaded.getDescription());
           
            
    }

    public void testToString() {
            PackageItem rulePackageItem1 = getRepo().createRulePackage("testToStringPackage", "desc");
            
            AssetItem ruleItem1 = rulePackageItem1.addAsset("testToStringPackage", "test lhs content" );
            ruleItem1.updateContent( "test lhs content" );
            
            assertNotNull(rulePackageItem1.toString());                        

    }
    
    public void testRemoveRule() {
            PackageItem rulePackageItem1 = getRepo().createRulePackage("testRemoveRule", "desc");
            
            AssetItem ruleItem1 = rulePackageItem1.addAsset("testRemoveRule", "test lhs content" );
            ruleItem1.updateContent( "test lhs content" ); 
            
            
            
            Iterator rulesIt = rulePackageItem1.getRules();
            AssetItem next = (AssetItem) rulesIt.next();
            
            assertFalse(rulesIt.hasNext());
            assertEquals("testRemoveRule", next.getName());
                               
            
            
            ruleItem1.updateContent("new lhs");
            List rules = iteratorToList(rulePackageItem1.getRules());
            assertNotNull(rules);
            assertEquals(1, rules.size());
            assertEquals("testRemoveRule", ((AssetItem)rules.get(0)).getName());
            assertEquals("new lhs", ((AssetItem)rules.get(0)).getContent());
                        
            AssetItem ruleItem2 = rulePackageItem1.addAsset("testRemoveRule2", "test lhs content");
            
            //remove the rule, make sure the other rule in the pacakge stays around
            rulePackageItem1.removeRule(ruleItem1.getName());
            rulePackageItem1.rulesRepository.save();
            rules = iteratorToList(rulePackageItem1.getRules());
            assertEquals(1, rules.size());
            assertEquals("testRemoveRule2", ((AssetItem)rules.get(0)).getName());
            
            //remove the rule that is following the head revision, make sure the pacakge is now empty
            rulePackageItem1.removeRule(ruleItem2.getName());
            rules = iteratorToList(rulePackageItem1.getRules());
            assertNotNull(rules);
            assertEquals(0, rules.size());

    }
    
 
    
    public void testRemoveFunction() {
            PackageItem rulePackageItem1 = getRepo().createRulePackage("testRemoveFunction", "yayayaya");
            
            FunctionItem functionItem1 = getRepo().addFunction("testRemoveFunction", "test content");
            
            rulePackageItem1.addFunction(functionItem1);
            
            List functions = rulePackageItem1.getFunctions();
            assertNotNull(functions);
            assertEquals(1, functions.size());
            assertEquals("testRemoveFunction", ((FunctionItem)functions.get(0)).getName());
                                    
            functionItem1.updateContent("new content");
            functions = rulePackageItem1.getFunctions();
            assertNotNull(functions);
            assertEquals(1, functions.size());
            assertEquals("testRemoveFunction", ((FunctionItem)functions.get(0)).getName());
            assertEquals("new content", ((FunctionItem)functions.get(0)).getContent());
            
            FunctionItem functionItem2 = getRepo().addFunction("testRemoveFunction2", "test content");
            rulePackageItem1.addFunction(functionItem2);
            
            //remove the function, make sure the other function in the package stays around
            rulePackageItem1.removeFunction(functionItem1);
            functions = rulePackageItem1.getFunctions();
            assertEquals(1, functions.size());
            assertEquals("testRemoveFunction2", ((FunctionItem)functions.get(0)).getName());
            
            //remove the function that is following the head revision, make sure the package is now empty
            rulePackageItem1.removeFunction(functionItem2);
            functions = rulePackageItem1.getFunctions();
            assertNotNull(functions);
            assertEquals(0, functions.size());

    }
    
    
    public void testGetFormat() {        
            PackageItem rulePackageItem1 = getRepo().createRulePackage("testGetFormat", "woot");
            assertNotNull(rulePackageItem1);
            assertEquals("Rule Package", rulePackageItem1.getFormat());    

    }        
}