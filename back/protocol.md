## This is the Cadaver Exquisito protocol overview

### Payload (JSON format)

A game's payload is such as

{
    "id": 0
    "URL": "https://xxx.xx
    "players": [
        {
        "id": 0,
        "name": "a name",
        "avatar": "a filename",
        "isAdmin": true
        },
        {
        "id": 1,
        "name": "another name",
        "avatar": "another filename",
        "isAdmin": false
        },
        "id": 2,
        "name": "yet another name",
        "avatar": "yet another filename",
        "isAdmin": false
        },
    ]
    "canvassequence": {
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
    "activeCanvasSequence": 1,
    "isLastCanvasSequence": false,
    "drawings": ["ajointdrawing", "anotherjointdrawing", "yetanotherjointdrawing], 
    "status": "ongoing"

}

#### Notes

- A game status can be ["waiting", "ongoing", "finished", "error"]
- game, players and canvas id are numeric
- filenames are simple filenames, not relative or absolute path included
- canvassequence is populated upon change from awaiting to ongoing status