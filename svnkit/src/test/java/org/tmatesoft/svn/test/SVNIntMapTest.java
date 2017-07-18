package org.tmatesoft.svn.test;

import org.junit.Assert;
import org.junit.Test;
import org.tmatesoft.svn.core.internal.util.SVNIntMap;

public class SVNIntMapTest {

    @Test
    public void testMapIsInitiallyEmpty() throws Exception {
        final SVNIntMap map = new SVNIntMap();
        for (int i = 0; i < SVNIntMap.INITIAL_CAPACITY * 2; i++) {
            Assert.assertNull(map.get(i));
        }
    }

    @Test
    public void testBasics() throws Exception {
        final SVNIntMap map = new SVNIntMap();

        final Object o0 = new Object();
        final Object o1 = new Object();
        final Object o2 = new Object();
        final Object o3 = new Object();

        map.put(0, o0);
        map.put(1, o1);
        map.put(2, o2);
        map.put(3, o3);

        Assert.assertEquals(o0, map.get(0));
        Assert.assertEquals(o1, map.get(1));
        Assert.assertEquals(o2, map.get(2));
        Assert.assertEquals(o3, map.get(3));

        Assert.assertTrue(map.containsKey(0));
        Assert.assertTrue(map.containsKey(1));
        Assert.assertTrue(map.containsKey(2));
        Assert.assertTrue(map.containsKey(3));
    }

    @SuppressWarnings("PointlessArithmeticExpression")
    @Test
    public void testCollisions() throws Exception {
        final SVNIntMap map = new SVNIntMap();

        final Object o0 = new Object();
        final Object o1 = new Object();
        final Object o2 = new Object();
        final Object o3 = new Object();

        map.put(0 * SVNIntMap.CACHE_SIZE, o0);
        map.put(1 * SVNIntMap.CACHE_SIZE, o1);
        map.put(2 * SVNIntMap.CACHE_SIZE, o2);
        map.put(3 * SVNIntMap.CACHE_SIZE, o3);

        Assert.assertEquals(o0, map.get(0 * SVNIntMap.CACHE_SIZE));
        Assert.assertEquals(o1, map.get(1 * SVNIntMap.CACHE_SIZE));
        Assert.assertEquals(o2, map.get(2 * SVNIntMap.CACHE_SIZE));
        Assert.assertEquals(o3, map.get(3 * SVNIntMap.CACHE_SIZE));

        Assert.assertTrue(map.containsKey(0 * SVNIntMap.CACHE_SIZE));
        Assert.assertTrue(map.containsKey(1 * SVNIntMap.CACHE_SIZE));
        Assert.assertTrue(map.containsKey(2 * SVNIntMap.CACHE_SIZE));
        Assert.assertTrue(map.containsKey(3 * SVNIntMap.CACHE_SIZE));
    }

    @Test
    public void testGrowCapacity() throws Exception {
        final SVNIntMap map = new SVNIntMap();

        final Object[] objects = new Object[SVNIntMap.INITIAL_CAPACITY * 2];

        for (int i = 0; i < objects.length; i++) {
            objects[i] = new Object();
            map.put(i, objects[i]);
            Assert.assertTrue(map.containsKey(i));
            Assert.assertEquals(objects[i], map.get(i));
        }
        //check the same but after
        for (int i = 0; i < objects.length; i++) {
            Assert.assertTrue(map.containsKey(i));
            Assert.assertEquals(objects[i], map.get(i));
        }
    }

    @Test
    public void testRewriteValues() throws Exception {
        final SVNIntMap map = new SVNIntMap();

        int sameKey = 16;

        for (int i = 0; i < SVNIntMap.INITIAL_CAPACITY * 2; i++) {
            final Object object = new Object();
            map.put(16, object);
            Assert.assertEquals(object, map.get(16));
        }
    }
}
