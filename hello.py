from flask import Flask, render_template, request, redirect, url_for
from configDb import *
app =  Flask(__name__)

#altaInventario
@app.route('/altaInventario' , methods=['GET', 'POST'])
def index():
    print("entre al alta de inventario ");
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
        
            conexion_MySQLdb.commit()
            cursor.close()
            conexion_MySQLdb.close()
        
    return render_template('index.html');

#actualizarInventario
@app.route('/actualizarInventario' , methods=['GET', 'POST'])
def actualizarInventario():
    print("entre al actualizarInventario ");
    menuActualiza = request.form.get('menuActualiza');
 
    print("QUE LLEVA EL MENU ACTUALIZA: "  + menuActualiza);
      
    if menuActualiza == '0':
        barcode = request.form.get('codigoBarras');
        price = request.form.get('precio');
        id = request.form.get('id');
        quantity = request.form.get('cantidad');
        product_name = request.form.get('nombreProducto');
        
        print("1.- Act :" + menuActualiza + ", " + barcode + ", " + price + ", " + quantity + ", " + product_name);
        return render_template('edit_products.html', id = id, barcode = barcode, price = price, quantity = quantity, product_name = product_name);
    elif menuActualiza == '1':
            barcode = request.form.get('barcode');
            price = request.form.get('price');
            id = request.form.get('id');
            quantity = request.form.get('quantity');
            product_name = request.form.get('product_name');
            print("2-. Act :" + menuActualiza);
            print("Voy a actualizar el id: ");
            print(id);
            print(product_name);
            print(price);
            print(quantity);
            
            conexion_MySQLdb = conectionDB()
            with conexion_MySQLdb.cursor() as cursor:
                cursor.execute("UPDATE products SET product_name = %s, quantity = %s, price = %s WHERE id = %s", 
                (product_name, quantity, price, id))   
            descAct = "Producto actualizado correctamente";
            conexion_MySQLdb.commit()
            cursor.close()
            conexion_MySQLdb.close()
            return render_template('edit_products.html', id = id, barcode = barcode, price = price, quantity = quantity, product_name = product_name, descAct=descAct);
                     
    else:
        return redirect("/consultaInventario"); 



#consultaInventario
@app.route('/consultaInventario',methods=['GET', 'POST'])
def hello():
    #print(request.form);
    products = []
    conexion_MySQLdb = conectionDB()
        
    with conexion_MySQLdb.cursor() as cursor:
        cursor.execute("select id, barcode, product_name, quantity, price FROM products")
        products = cursor.fetchall()     
        print(products)
    conexion_MySQLdb.close()
      
    return render_template('search_products.html', products = products);

#eliminaInventario  
@app.route('/eliminaInventario', methods=['GET', 'POST'])
def eliminaInventario():
    conexion_MySQLdb = conectionDB()
    products = []
    if request.method == 'POST':
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
                #print(products)
                conexion_MySQLdb.commit()
        else:
            print("Error: El id proporcionado no es válido.")
        cursor.close();
        conexion_MySQLdb.close()

    return render_template('search_products.html', products = products)


    
#Menus 
@app.route('/menu' , methods=['GET','POST'])
def menuController():

    if request.method == 'POST':
        menuAlta = request.form.get('menuAlta')
        menuConsulta = request.form.get('menuConsulta')
        if menuAlta == '1':
            #print("Entro aca alta: " + menuAlta)
            return redirect("/altaInventario");
        if menuConsulta == '2':
            #print("Entro aca consulta: " + menuConsulta)
            return redirect("/consultaInventario");   
    return render_template('menu.html');