package com.project.lol.webview.injections

object LibraryParser {
    const val CONTENT = """
            window.parseLibrary = function(items) {
                var res={playlists:[],albums:[],artists:[],podcasts:[]};
                items.forEach(function(entry){
                    var data = entry.item && entry.item.data;
                    if(!data || !data.__typename) return;
                    switch(data.__typename) {
                        case 'PseudoPlaylist':
                        case 'Playlist':
                            res.playlists.push({id:data.uri,name:data.name,image:data.images&&data.images.items&&data.images.items[0]&&data.images.items[0].sources&&data.images.items[0].sources[0]?data.images.items[0].sources[0].url:null});
                            break;
                        case 'Album':
                            res.albums.push({id:data.uri,name:data.name,image:data.coverArt&&data.coverArt.sources?data.coverArt.sources[0].url:null,artists:data.artists&&data.artists.items?data.artists.items.map(function(a){return a.profile&&a.profile.name}).filter(Boolean):[]});
                            break;
                        case 'Artist':
                            res.artists.push({id:data.uri,name:data.profile&&data.profile.name,image:data.visuals&&data.visuals.avatarImage&&data.visuals.avatarImage.sources?data.visuals.avatarImage.sources[0].url:null});
                            break;
                        case 'Podcast':
                            res.podcasts.push({id:data.uri,name:data.name,image:data.coverArt&&data.coverArt.sources?data.coverArt.sources[0].url:null,artists:data.publisher&&data.publisher.name?[data.publisher.name]:[]});
                            break;
                    }
                });
                return res;
            };
        
    """
}
