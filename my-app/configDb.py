import mysql.connector

def conectionDB():
    
    mydb = mysql.connector.connect(
    host="b0b6j2pllertpbskfvl6-mysql.services.clever-cloud.com", 
    user ="ujl512i9hwxc7y1r",
    passwd = "SK4kd6Mhff5wZClLnvc1",
    database = "b0b6j2pllertpbskfvl6"  
    )
    
    if mydb:
        print ("conexion exitosa");
    else:
        print ("conexion error");
    return mydb