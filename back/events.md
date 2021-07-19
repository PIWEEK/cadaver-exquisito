- Server starts

Available [createGame]

- Listen for createGame
    * createGame
    * joinGame (first player isAdmin=true)
    * game.status = waiting
    * game.url is set
    * game.time is set


Available [joinGame, startGame, finishGame]

- Listen for joinGame
    * joinGame (player isAdmin=false)
    * send payload to all clients

- Listen for finishGame (from player isAdmin=true)
    * send payload to all clients
    * send endGame to all clients

- Listen for startGame (only from player isAdmin=true)
    * send payload to all clients
    * send startTurn to all clients
    * game.status = ongoing
    * no more joinGame events can occur

Available [endTurn, finishGame, sendPlayerCanvas]

- Listen for endTurn (from player isAdmin=true)
    * send endTurn to all clients
    * wait for all client sendPlayerCanvas
    * send payload to all clients
    * send startTurn to all clients

- Listen for sendPlayerCanvas (from any player)

- Listen for finishGame (from player isAdmin=true)
    * send payload to all clients
    * send endGame to all clients
