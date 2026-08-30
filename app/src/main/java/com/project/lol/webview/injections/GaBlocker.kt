package com.project.lol.webview.injections

/*
 * CREDIT: uBlock Origin (Raymond Hill) - Google Analytics Neutralizer
 * Source: https://github.com/gorhill/uBlock
 *
 * Replaces window.ga with a noop that still fires hitCallbacks
 * so pages depending on GA callbacks don't break. Empties the
 * existing ga.q queue and patches dataLayer.push the same way.
 */

object GaBlocker {
    const val CONTENT = """
        (function(){
            'use strict';

            var noopfn = function(){};

            var Tracker = function(){};
            var p = Tracker.prototype;
            p.get = noopfn;
            p.set = noopfn;
            p.send = noopfn;

            var w = window;
            var gaName = w.GoogleAnalyticsObject || 'ga';
            var gaQueue = w[gaName];

            var ga = function(){
                var len = arguments.length;
                if (len === 0) return;
                var a = arguments[len-1];
                var fn;
                if (a && typeof a === 'object' && typeof a.hitCallback === 'function') {
                    fn = a.hitCallback;
                } else if (typeof a === 'function') {
                    fn = function(){ a(ga.create()); };
                } else {
                    for (var i = 0; i < len; i++) {
                        if (arguments[i] === 'hitCallback' && i+1 < len && typeof arguments[i+1] === 'function') {
                            fn = arguments[i+1];
                            break;
                        }
                    }
                }
                if (typeof fn !== 'function') return;
                try { fn(); } catch(ex){}
            };
            ga.create = function(){ return new Tracker(); };
            ga.getByName = function(){ return new Tracker(); };
            ga.getAll = function(){ return [new Tracker()]; };
            ga.remove = noopfn;
            ga.loaded = true;
            w[gaName] = ga;

            var dl = w.dataLayer;
            if (dl && typeof dl === 'object') {
                if (dl.hide && typeof dl.hide.end === 'function') {
                    dl.hide.end();
                    dl.hide.end = function(){};
                }
                if (typeof dl.push === 'function') {
                    var doCallback = function(item){
                        if (!item || typeof item !== 'object') return;
                        if (typeof item.eventCallback !== 'function') return;
                        setTimeout(item.eventCallback, 1);
                        item.eventCallback = function(){};
                    };
                    var originalPush = dl.push;
                    dl.push = function(){
                        doCallback(arguments[0]);
                        return originalPush.apply(this, arguments);
                    };
                    if (Array.isArray(dl)) {
                        var q = dl.slice();
                        for (var i = 0; i < q.length; i++) {
                            doCallback(q[i]);
                        }
                    }
                }
            }

            if (typeof gaQueue === 'function' && Array.isArray(gaQueue.q)) {
                var q2 = gaQueue.q.slice();
                gaQueue.q.length = 0;
                for (var j = 0; j < q2.length; j++) {
                    ga.apply(null, q2[j]);
                }
            }

            try { AndBridge.dbg('s', 'GA tracker neutralized'); } catch(e){}
        })();
    """
}