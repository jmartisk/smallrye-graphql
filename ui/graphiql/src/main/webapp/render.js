const api = "/graphql";
const defaultQuery = "";
const headerEditorEnabled = true;
const shouldPersistHeaders = false;

const urlParams = new URLSearchParams(window.location.search);


// Parse the search string to get url parameters.
var search = window.location.search;
var parameters = {};
search
    .substr(1)
    .split("&")
    .forEach(function (entry) {
        var eq = entry.indexOf("=");
        if (eq >= 0) {
            parameters[decodeURIComponent(entry.slice(0, eq))] =
                decodeURIComponent(entry.slice(eq + 1));
        }
    });

// If variables was provided, try to format it.
if (parameters.variables) {
    try {
        parameters.variables = JSON.stringify(
            JSON.parse(parameters.variables),
            null,
            2
        );
    } catch (e) {
        // Do nothing, we want to display the invalid JSON as a string, rather
        // than present an error.
    }
}

// If headers was provided, try to format it.
if (parameters.headers) {
    try {
        parameters.headers = JSON.stringify(
            JSON.parse(parameters.headers),
            null,
            2
        );
    } catch (e) {
        // Do nothing, we want to display the invalid JSON as a string, rather
        // than present an error.
    }
}

// When the query and variables string is edited, update the URL bar so
// that it can be easily shared.
//function onEditQuery(newQuery) {
//    updateURL();
//}

function onEditVariables(newVariables) {
    parameters.variables = newVariables;
    updateURL();
}


function onEditHeaders(newHeaders) {
    parameters.headers = newHeaders;
    updateURL();
}

function updateURL() {
    var newSearch =
        "?" +
        Object.keys(parameters)
            .filter(function (key) {
                return Boolean(parameters[key]);
            })
            .map(function (key) {
                return (
                    encodeURIComponent(key) +
                    "=" +
                    encodeURIComponent(parameters[key])
                );
            })
            .join("&");
    history.replaceState(null, null, newSearch);
}

var defaultHeaders = {
    "Accept": "application/json",
    "Content-Type": "application/json"
};

// Defines a GraphQL fetcher using the fetch API. You're not required to
// use fetch, and could instead implement graphQLFetcher however you like,
// as long as it returns a Promise or Observable.
function graphQLFetcher(graphQLParams) {
    let mergedHeaders;
    if (
        typeof parameters.headers === "undefined" ||
        parameters.headers === null ||
        parameters.headers.trim() === ""
    ) {
        mergedHeaders = defaultHeaders;
    } else {
        mergedHeaders = {
            ...defaultHeaders,
            ...JSON.parse(parameters.headers),
        };
    }

    var query = graphQLParams.query;

    if(query.startsWith("subscription ")) {
        var new_uri = getWsUrl();
        var initialized = false;
        observable = new rxjs.Observable((observer) => {
            webSocket = new WebSocket(url = new_uri, protocols = ["graphql-transport-ws", "graphql-ws"]);
            observer.next("Initializing a connection to the server...");

            webSocket.onopen = function() {
                if(webSocket.protocol === "graphql-transport-ws") {
                    webSocket.send(JSON.stringify({type: "connection_init"}));
                    webSocket.onmessage = function (event) {
                        let data = JSON.parse(event.data);
                        switch(data["type"]) {
                            case 'connection_ack':
                                initialized = true;
                                let startMessage = {
                                    id: "1",
                                    type: "subscribe",
                                    payload: parameters
                                };
                                webSocket.send(JSON.stringify(startMessage));
                                observer.next("Connection initialized (protocol=graphql-transport-ws), requested a subscription...")
                                break;
                            case 'next':
                                observer.next(data.payload);
                                break;
                            case 'complete':
                                webSocket.close();
                                break;
                            case 'ping':
                                webSocket.send(JSON.stringify({
                                    type: "pong"
                                }));
                                break;
                            case 'pong':
                                break;
                            case 'error':
                                observer.next(data.payload);
                                webSocket.close();
                            default:
                                observer.next(data);
                                break;
                        }
                    };

                } else if(webSocket.protocol === "graphql-ws") {
                    webSocket.send(JSON.stringify({type: "connection_init"}));
                    webSocket.onmessage = function (event) {
                        let data = JSON.parse(event.data);
                        switch(data["type"]) {
                            case 'connection_ack':
                                initialized = true;
                                let startMessage = {
                                    id: "1",
                                    type: "start",
                                    payload: graphQLParams
                                };
                                webSocket.send(JSON.stringify(startMessage));
                                observer.next("Connection initialized (protocol=graphql-ws), requested a subscription...")
                                break;
                            case 'data':
                                observer.next(data.payload);
                                break;
                            case 'complete':
                                webSocket.close();
                                break;
                            case 'ka':
                                break;
                            case 'error':
                                observer.next(data);
                                webSocket.close();
                            default:
                                observer.next(data);
                                break;
                        }
                    };
                } else {
                    observer.next("ERROR: Server picked an unknown subprotocol: " + webSocket.protocol);
                }
            };
            webSocket.onerror = function(err) {
                observer.error(JSON.stringify(err, null, 4));
            };
            webSocket.onclose = function(event){
                observer.complete();
                observable = null;
            };
            return {
                unsubscribe() {
                    if(initialized) {
                        if(webSocket.protocol === "graphql-transport-ws") {
                            webSocket.send(JSON.stringify({
                                id: "1",
                                type: "complete"
                            }));
                        } else if(webSocket.protocol === "graphql-ws") {
                            webSocket.send(JSON.stringify({
                                id: "1",
                                type: "stop"
                            }));
                            webSocket.send(JSON.stringify({
                                type: "connection_terminate"
                            }));
                        }
                    }
                    webSocket.close();
                    webSocket = null;
                    observable = null;
                }
            };
        });
        return observable;
    } else {
        return fetch(api, {
            method: 'post',
            headers: mergedHeaders,
            body: JSON.stringify(parameters),
        }).then(function (response) {
            console.log("Fetched with operationName: " + parameters.operationName)
            return response.text();
        }).then(function (responseBody) {
            try {
                return JSON.parse(responseBody);
            } catch (error) {
                return responseBody;
            }
        });
    }

    // FIXME: this is the way using GraphiQL.createFetcher provided by graphiql
    // but with it we hit the https://github.com/graphql/graphiql/issues/1807 issue.
    // So we are working around it by calling fetch directly above,
    // and handling websockets for subscriptions manually

    // return GraphiQL.createFetcher({
    //     url: getUrl(),
    //     subscriptionUrl: getWsUrl(),
    //     headers: mergedHeaders,
    // });

}

function getWsUrl() {
    var new_uri;
    if (window.location.protocol === "https:") {
        new_uri = "wss:";
    } else {
        new_uri = "ws:";
    }
    new_uri += "//" + window.location.host + api;

    return new_uri;
}

function getUrl() {
    return window.location.protocol + "//" + window.location.host + api;
}

function GraphiQLWithExplorer() {
//        var [query, setQuery] = React.useState(parameters.query);
        const onEditQuery = (newQuery) => {
            parameters.query = newQuery;
//           setQuery(parameters.query);
            updateURL();
        }

//        var explorerPlugin = GraphiQLPluginExplorer.useExplorerPlugin({
//          query: parameters.query,
//          onEdit: onEditQuery,
//        });

        const onEditOperationName = (newOperationName) => {
            console.log("onEditOperationName called");
            parameters.operationName = newOperationName;
            updateURL();
            console.log(parameters);
//            setQuery(parameters.query);
        }
        return React.createElement(GraphiQL, {
                       fetcher: graphQLFetcher,
                       query: parameters.query,
                       variables: parameters.variables,
                       headers: parameters.headers,
                       operationName: parameters.operationName,
                       onEditOperationName: onEditOperationName,
                       onEditVariables: onEditVariables,
                       onEditHeaders: onEditHeaders,
                       onEditQuery: onEditQuery,
                       defaultSecondaryEditorOpen: true,
                       headerEditorEnabled: headerEditorEnabled,
                       shouldPersistHeaders: shouldPersistHeaders,
                       defaultQuery: defaultQuery,
//                       plugins: [explorerPlugin],
                       defaultEditorToolsVisibility: true,
                   });
      }

ReactDOM.render(React.createElement(GraphiQLWithExplorer),
    document.getElementById("graphiql")
);