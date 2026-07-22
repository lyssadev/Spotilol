package com.project.lol.webview.injections

object LibraryFetcher {
    const val CONTENT = """
            window.fetchAllLibrary = async function(){
                var limit=50, offset=0, allItems=[], hasMore=true;
                while(hasMore){
                    var resp = await oriFetch('https://api-partner.spotify.com/pathfinder/v2/query',{
                        method:'POST',
                        headers:{
                            'Authorization':window.spotAuthToken,
                            'Client-Token':window.spotCliToken,
                            'Content-Type':'application/json;charset=UTF-8'
                        },
                        body:JSON.stringify({
                            variables:{
                                filters:[],order:null,textFilter:'',
                                features:['LIKED_SONGS','YOUR_EPISODES_V2','PRERELEASES','EVENTS'],
                                limit:limit,offset:offset,flatten:false,expandedFolders:[],
                                folderUri:null,includeFoldersWhenFlattening:true
                            },
                            operationName:'libraryV3',
                            extensions:{persistedQuery:{version:1,sha256Hash:'0082bf82412db50128add72dbdb73e2961d59100b9cbf41fb25c568bd8bc358b'}}
                        })
                    });
                    var data = await resp.json();
                    var items = (data && data.data && data.me && data.me.libraryV3 && data.me.libraryV3.items) || [];
                    allItems = allItems.concat(items);
                    if(items.length < limit) hasMore=false; else offset+=limit;
                }
                return allItems;
            };
        
    """
}
