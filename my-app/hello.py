from flask import Flask, render_template, request, redirect, url_for
from configDb import *
app =  Flask(__name__)

@app.route('/altaInventario' , methods=['GET', 'POST'])
def index():
    print(request.form);
    barcode = request.form.get('barcode');
    price = request.form.get('price');
    product_name = request.form.get('productName');
    quantity = request.form.get('quantity');
    
    if(request.method == 'POST'):
        

        print(request.form);
        print(barcode);
      
        if barcode is None:
           return render_template('index.html');
        else:
            conexion_MySQLdb = conectionDB()
            print(price);
            print(product_name);
            print(quantity);
            with conexion_MySQLdb.cursor() as cursor:
                cursor.execute("INSERT INTO products (barcode, product_name, quantity, price) VALUES( %s,  %s, %s, %s)", 
                (barcode, product_name, quantity,price ))     
        
             #sql  = ("INSERT INTO products (barcode, product_name, quantity, price) VALUES( %s,  %s, %s, %s,)")
             #valores = (barcode, product_name, quantity,price )
             #cursor.execute(sql, valores);
            conexion_MySQLdb.commit()
            #cursor.close()
            conexion_MySQLdb.close()
        
    return render_template('index.html');

@app.route('/consultaInventario',methods=['GET', 'POST'])
def hello():
    print(request.form);
    products = []
    conexion_MySQLdb = conectionDB()
    print(request.form);
        
    with conexion_MySQLdb.cursor() as cursor:
        cursor.execute("select id, barcode, product_name, quantity, price FROM products")
        products = cursor.fetchall()     
        print(products)
    cursor.close();
    conexion_MySQLdb.close()
    
        
    return render_template('search_products.html', products = products);

@app.route('/eliminaInventario', methods=['GET', 'POST'])
def eliminaInventario():
    conexion_MySQLdb = conectionDB()
    products = []
    if request.method == 'POST':
        print(request.form)  # Depurar el contenido del formulario
        id = request.form.get('id')

        # Validar que el id sea un entero válido
        if id and id.isdigit():
            id = int(id)
            with conexion_MySQLdb.cursor() as cursor:
                cursor.execute("DELETE FROM products WHERE id = %s", (id,))
            conexion_MySQLdb.commit()
            with conexion_MySQLdb.cursor() as cursor:
                cursor.execute("select id, barcode, product_name, quantity, price FROM products")
                products = cursor.fetchall()     
                print(products)
                conexion_MySQLdb.commit()
        else:
            print("Error: El id proporcionado no es válido.")
        cursor.close();
        conexion_MySQLdb.close()

    return render_template('search_products.html', products = products)

#Menu inventario
@app.route('/menuInventario' , methods=['GET'])
def menuInventario():
    print("Entro aca menuInventario")
    if request.method == 'GET':
        return render_template('index.html');
    
#Menus 
@app.route('/menu' , methods=['GET','POST'])
def menuController():
    print("***** Entro aca menu")
    if request.method == 'POST':
        menuAlta = request.form.get('menuAlta')
        menuConsulta = request.form.get('menuConsulta')
        if menuAlta == '1':
            print("Entro aca menu: " + menuAlta)
            return render_template('index.html');
        if menuConsulta == '2':
            print("Entro aca menu: " + menuConsulta)
            return render_template('search_poducts.html');
        

    return render_template('menu.html');