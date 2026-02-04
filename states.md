1. Actual DEAD, expected to be DEAD. 
    1. Dead due to lastCommandReceiveTime beeing to large.
2. Actual DEAD, expected to be ALIVE.
    1. Due to never sending any ACK back
3. Actual ALIVE, expected to be ALIVE.
    1. Due to confirming config and never confirming other command.
    2. Due to confirming config
4. Actual ALIVE, expected to be DEAD