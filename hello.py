from flask import Flask, render_template, request, redirect, url_for, jsonify
import requests
from datetime import  datetime
from configDb import *
app =  Flask(__name__)


# Tu API key de PricesAPI
API_KEY = "pricesapi_UqgptyPtRfonJsOJMj4FHcPbBjW1Al"  # <--- reemplaza con tu clave real

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
            cursor = conexion_MySQLdb.cursor()
            #print(price);
            #print(product_name);
            #print(quantity);
            try:
                cursor.execute(
                    "INSERT INTO products (barcode, product_name, quantity, price) VALUES (%s, %s, %s, %s)", 
                    (barcode, product_name, quantity, price)
                )
                conexion_MySQLdb.commit()
            finally:
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
        url_img_product=request.form.get('url_img_product');
        
        print("1.- Act :" + menuActualiza + ", " + barcode + ", " + price + ", " + quantity + ", " + product_name + "," + url_img_product);
        return render_template('edit_products.html', id = id, barcode = barcode, price = price, quantity = quantity, product_name = product_name,url_img_product= url_img_product);
    elif menuActualiza == '1':
            barcode = request.form.get('barcode');
            price = request.form.get('price');
            id = request.form.get('id');
            quantity = request.form.get('quantity');
            product_name = request.form.get('product_name');
            url_img_product =  request.form.get('url_img_product');
            print("2-. Act :" + menuActualiza);
            print("Voy a actualizar el id: ");
            print(id);
            print(product_name);
            print(price);
            print(quantity);
            print(url_img_product)
            conexion_MySQLdb = conectionDB()
            cursor = conexion_MySQLdb.cursor()
            try:
                cursor.execute(
                    "UPDATE products SET product_name = %s, quantity = %s, price = %s, url_img_product = %s WHERE id = %s",
                    (product_name, quantity, price, url_img_product, id)
                )
                descAct = "Producto actualizado correctamente"
                conexion_MySQLdb.commit()
            finally:
                cursor.close()
            conexion_MySQLdb.close()
            return render_template('edit_products.html', id = id, barcode = barcode, price = price, quantity = quantity, product_name = product_name, descAct=descAct, url_img_product= url_img_product);
                     
    else:
        return redirect("/consultaInventario"); 



#consultaInventario
@app.route('/consultaInventario',methods=['GET', 'POST'])
def hello():
    #print(request.form);
    products = []
    conexion_MySQLdb = conectionDB()
        
    cursor = conexion_MySQLdb.cursor()
    try:
        cursor.execute("SELECT id, barcode, product_name, quantity, price, url_img_product  FROM products order by product_name asc")
        products = cursor.fetchall()
        #print(products)
    finally:
        cursor.close()
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
            cursor = conexion_MySQLdb.cursor()
            try:
                cursor.execute("DELETE FROM products WHERE id = %s", (id,))
                conexion_MySQLdb.commit()
            finally:
                cursor.close()

            cursor = conexion_MySQLdb.cursor()
            try:
                cursor.execute("SELECT id, barcode, product_name, quantity, price, url_img_product FROM products order by product_name asc")
                products = cursor.fetchall()
                #print(products)
                conexion_MySQLdb.commit()
            finally:
                cursor.close()
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
            return redirect("/altaInventario");
        if menuConsulta == '2':
            return redirect("/consultaInventario");   
        if menuConsulta == '3':
            #print("Entro aca consulta: " + menuConsulta)
            return redirect("/consultaVentaInventario");   
    return render_template('menu.html');

#ventaInventario
@app.route('/ventaInventario' , methods=['GET', 'POST'])
def ventaInventario():
    print("entre al ventaInventario ");
    menuActualiza = request.form.get('menuActualiza');
 
    print("QUE LLEVA EL MENU ACTUALIZA: "  + menuActualiza);
      
    if menuActualiza == '0':
        barcode = request.form.get('codigoBarras');
        price = request.form.get('precio');
        id_product = request.form.get('id_product');
        quantity = request.form.get('cantidad');
        product_name = request.form.get('nombreProducto');
        url_img_product=request.form.get('url_img_product');
        
        print("voy a vender :" + menuActualiza + ", " + barcode + ", " + price + ", " + quantity + ", " + product_name + ", " + id_product);
        return render_template('sales_products.html', id_product = id_product, barcode = barcode, price = price, quantity = quantity, product_name = product_name);
    elif menuActualiza == '1':
            barcode = request.form.get('barcode');
            price = request.form.get('price');
            id_product = request.form.get('id_product');
            quantity = request.form.get('quantity');
            product_name = request.form.get('product_name');
            date_sale =  datetime.now().strftime('%Y-%m-%d');
            comments = '';
            print(id_product); 
            print("2-. Act :" + menuActualiza);
            #print(id_product +  ", " + barcode + ", " + product_name + ", " + quantity + ", " + price + ", " + date_sale + ", " + comments);
            conexion_MySQLdb = conectionDB()
            cursor = conexion_MySQLdb.cursor()  
            try:
                cursor.execute(
                    "INSERT INTO products_sales_tita (id_product, barcode, product_name, quantity, price, date_sale, comments) VALUES (%s, %s, %s, %s, %s, %s, %s)", 
                    (id_product, barcode, product_name, quantity, price, date_sale, comments)
                )
                conexion_MySQLdb.commit()
            finally:
                cursor.close()
            conexion_MySQLdb.close()
            descAct = "Producto Vendido"
            return render_template('sales_products.html', id_product = id_product, barcode = barcode, price = price, quantity = quantity, product_name = product_name, descAct=descAct);
                     
    else:
        return redirect("/consultaInventario"); 
    
#consultaInventario
@app.route('/consultaVentaInventario',methods=['GET', 'POST'])
def consultaVentaInventario():
    #print(request.form);
    products = []
    conexion_MySQLdb = conectionDB()
        
    cursor = conexion_MySQLdb.cursor()
    try:
        cursor.execute("SELECT barcode, product_name, quantity, price, date_sale  FROM products_sales_tita order by date_sale desc")
        products = cursor.fetchall()
        #print(products)
    finally:
        cursor.close()
    conexion_MySQLdb.close()
      
    return render_template('sales_search_products.html', products = products);

# Busca ofertas por id
@app.route("/ofertas", methods=['GET', 'POST'])
def obtener_ofertas():
    """
    Devuelve el JSON de ofertas desde PricesAPI
    Parámetro opcional: ?q=producto
    """
    product_name = request.form.get('productName')
    print("---")
    print(product_name)
    print("---")

  
    if product_name is None or product_name.strip() == "":
        return render_template('search_offers.html')
         
    else:
            
        if request.method == 'POST':
            producto = request.args.get("q", product_name)  # default "iphone"
            url = "https://api.pricesapi.io/api/v1/products/search"
            params = {
                "q": producto,
                "country": "mx",
                "api_key": API_KEY
            }

            response = requests.get(url, params=params)

            if response.status_code == 200:
                data = response.json()
                # print(data["data"])  # Verifica la estructura real
                products = []
                results = data.get("data", {}).get("results", [])
                for item in results:
                    if "id" in item and "title" in item:
                        products.append({
                            "id": item["id"],
                            "title": item["title"],
                            "image": item["image"],
                            "offerCount": item["offerCount"]
                        })
                print(products)
                return render_template('search_offers.html', products=products)
            else:
                return jsonify({
                    "error": "No se pudo obtener la data",
                    "status_code": response.status_code
                })
                

# Busca ofertas
@app.route("/ofertasById", methods=['GET', 'POST'])
def obtener_ofertasById():
    """
    Devuelve el JSON de ofertas desde PricesAPI
    Parámetro opcional: ?q=producto
    """
    productId = request.form.get('product-id')
    print("-- ofertasById --")
    print(productId)
    print("---")

    if productId is None:
        return render_template('search_offersById.html')
    else:
        if request.method == 'POST':
            response = requests.get(
                f'https://api.pricesapi.io/api/v1/products/{productId}/offers',
                params={'country': 'mx'},
                headers={'x-api-key': API_KEY}
            )

            data = response.json()
            offers_data = data.get("data", {}).get("offers", [])
            print(offers_data)
            filtered_offers = []
            for item in offers_data:
                if (
                    "seller" in item and
                    "seller_url" in item and
                    "price" in item and
                    "currency" in item and
                    "url" in item and
                    "stock" in item and
                    "delivery_info" in item
                ):
                    filtered_offers.append({
                        "seller": item["seller"],
                        "seller_url": item["seller_url"], 
                        "price": float(item["price"]), 
                        "currency": item["currency"], 
                        "url": item["url"],
                        "stock": item["stock"],
                        "delivery_info": item["delivery_info"]
                    })
            # Ordenar por el precio más barato (ascendente)
            filtered_offers = sorted(filtered_offers, key=lambda x: x["price"])           
            return render_template('search_offersById.html', offers=filtered_offers)