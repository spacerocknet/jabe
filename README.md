jabe
====
I. Compile, build, and deploy
  1. Play Framework version 2.2.x
  2. To test:
    play run
  3. To deploy
    a. build: play clean compile dist
    b. deploy the file under target directory
    
II. API Service
  1. Get config
     Request: 
        GET /v1/quiz/config

     Response:
        {
            categories:  ["Geo", "Movies", "Musics"],
           battles_per_game: 5,
                   
        }   


  2. Get categories
     Request: 
        GET /v1/quiz/categories
     Response:
        ["Geo", "Movies", "Musics"]


  3. Get Random Quizzes for a single category:
     Request: 
      POST   /v1/quiz/request
        {
             userId: "abac1122",
             category: "Geography",
             size: 1
        }

     Response
       [ 
          {
            category: "Movies"
            qid:  12453
            question: "What is A?",
            answers: ["a", "b", "c", "d"]
          }
       ]

     Examples:
      curl -X POST  -H "Accept: Application/json" -H "Content-Type: application/json" --header "Authorization: Basic x"   http://localhost:9000/v1/quiz/request  -d '{"userId":1,"category":"Sports","num":1}'
      curl -X POST  -H "Accept: Application/json" -H "Content-Type: application/json" --header "Authorization: Basic x"   http://localhost:9000/v1/quiz/request  -d '{"userId":1,"category":"Musics","num":1}'


  4. Update a game's result 
      Request:
       POST /v1/quiz/result
         {
              userId:  "abc123",
              results:   "12345, 2342, 344"
         }
  
      Response:
         {  status: "Ok"}


  5. Add a question into the system
     Request:
      POST /quiz/add
       {
           qid:  1,
           category: "Movies",
           question: "Where is USA?",
           corrections: "North America",
           ans1:  "South Africa",
           ans2:  "Asia",
           ans3:  "Europe"
       }
