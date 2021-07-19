## This is the Cadaver Exquisito protocol overview

### Payload (JSON format)

A game's payload is such as

{
    "type": "payload",
    "gameId": 0
    "name": "a game name",
    "canvasWidth": 2048,
    "URL": "https://xxx.xx
    "players": [
        {
        "playerId": 0,
        "name": "a name",
        "avatar": "a filename",
        "isAdmin": true
        },
        {
        "playerId": 1,
        "name": "another name",
        "avatar": "another filename",
        "isAdmin": false
        },
        "playerId": 2,
        "name": "yet another name",
        "avatar": "yet another filename",
        "isAdmin": false
        },
    ]
    "canvasTurn": {
        0: {
            [
            "playerId:" 0,
            "canvasId": 0,
            "canvasBitmap": "a bitmap file"
            ],
            [
            "playerId:" 1,
            "canvasId": 3,
            "canvasBitmap": "another bitmap file"
            ],
            [
            "playerId:" 2,
            "canvasId": 6,
            "canvasBitmap": "yet another bitmap file"
            ],

        },
        1: {
            [
            "playerId:" 0,
            "canvasId": 1,
            "canvasBitmap": ""
            ],
            [
            "playerId:" 1,
            "canvasId": 4,
            "canvasBitmap": ""
            ],
            [
            "playerId:" 2,
            "canvasId": 7,
            "canvasBitmap": ""
            ],
        },
        2: {
            [
            "playerId:" 0,
            "canvasId": 2,
            "canvasBitmap": ""
            ],
            [
            "playerId:" 1,
            "canvasId": 5,
            "canvasBitmap": ""
            ],
            [
            "playerId:" 2,
            "canvasId": 8,
            "canvasBitmap": ""
            ],
        },

    }
    "activeCanvasTurn": 1,
    "isLastCanvasTurn": false,
    "drawings": ["ajointdrawing", "anotherjointdrawing", "yetanotherjointdrawing], 
    "status": "ongoing"

}

**End Turn**

{
    "type": "endTurn",
    "gameId": 0,
}

**Start Turn**

{
    "type": "startTurn",
    "gameId": 0,
}

**End Game**

{
    "type": "endGame",
    "gameId": 0,
}


**Error payload**

{
    "type": "error",
    "id": null,
    "error": "error message"
}

#### Notes

- A game status can be ["waiting", "ongoing", "finished", "error"]
- game, players and canvas id are numeric
- filenames are simple filenames, not relative or absolute path included
- canvasTurn is populated upon change from awaiting to ongoing status

### Messages

#### Server to client

Either returns payload or error payload (see above)

#### Client to Server

**createGame**

{
    "type": "createGame",
    "name": "a game name",
    "canvasWidth": 2048
}

**joinGame**

{
    "type": "joinGame",
    "name": "a player's name",
    "isAdmin": false,
    "gameId": 0
}

**startGame**

{
    "type": "startGame",
    "gameId": 0
}

**startTurn**

{
    "type": "startTurn",
    "gameId": 0
}


**endTurn**

{
    "type": "endTurn",
    "gameId": 0,
    "playerId": 0,
    "canvasBitmap": "a bitmap file"
    "order": 0
}

**sendPlayerCanvas**

{
    "type": "sendPlayerCanvas",
    "gameId": 0,
    "playerId": 0,
    "canvasBitmap": "a bitmap file"
    "order": 0
}

**showDrawings**

{
    "type": "showDrawings",
    "gameId": 0
}

**finishGame**

{
    "type": "finishGame",
    "gameId": 0
}