# Product Management Functionality

## Overview
The product management system handles product catalog display, RFID tag association, and product data retrieval for the shopping experience.

## Involved Files

### Backend Files
1. **ProductController.java** - REST API endpoints for product operations
2. **ProductService.java** - Business logic for product management
3. **Product.java** - Product entity
4. **ProductRepository.java** - Product data access
5. **RfidTag.java** - RFID tag entity
6. **RfidTagRepository.java** - RFID tag data access

### Frontend Files
1. **ShoppingHandheld.js** - Product display and selection
2. **api.js** - API client functions for product operations

### Database Tables
- **products**: Product catalog
- **rfid_tags**: RFID tag to product mapping
- **categories**: Product categories (if used)

## Process Flow

### Product Display

#### Step 1: Available Products List
- **File**: `ShoppingHandheld.js`
- **Constant**: `availableProducts` (lines 220-230)
- **Code**:
```javascript
const availableProducts = [
  { id: 1, name: 'Fresh Apples', brand: 'Farm Fresh', price: 120, mrp: 150, unit: '1 kg', rfidTag: 'RFID0001' },
  { id: 2, name: 'Bananas', brand: 'Organic', price: 50, mrp: 60, unit: '1 dozen', rfidTag: 'RFID0002' },
  { id: 3, name: 'Oranges', brand: 'Citrus Fresh', price: 85, mrp: 100, unit: '1 kg', rfidTag: 'RFID0003' },
  // ... more products
];
```
- **Static Data**: Pre-defined product list for demo

#### Step 2: Product Selection UI
- **File**: `ShoppingHandheld.js`
- **UI Rendering**: Product cards with add buttons (lines 400-500)
- **Code**:
```javascript
{availableProducts.map((product) => (
  <Card key={product.id} sx={{ mb: 2 }}>
    <CardContent>
      <Typography variant="h6">{product.name}</Typography>
      <Typography>Brand: {product.brand}</Typography>
      <Typography>Price: ₹{product.price}</Typography>
      <Typography>MRP: ₹{product.mrp}</Typography>
      <Typography>Unit: {product.unit}</Typography>
      <Button 
        variant="contained" 
        onClick={() => handleAddManualProduct(product)}
      >
        Add to Cart
      </Button>
    </CardContent>
  </Card>
))}
```
- **Display**: Name, brand, price, MRP, unit, add button

### RFID Tag Lookup

#### Step 1: RFID to Product Mapping
- **File**: `CartService.java`
- **Method**: `addItemByRfid` (lines 190-200)
- **Code**:
```java
// First, get product ID from RFID tag
String productSql = "SELECT product_id FROM rfid_tags WHERE rfid_tag = ? AND is_active = 1";
Long productId = jdbcTemplate.queryForObject(productSql, Long.class, rfidTag);

if (productId == null) {
    throw new IllegalArgumentException("Product not found for RFID tag: " + rfidTag);
}
```
- **Database Query**: RFID tag to product ID mapping

#### Step 2: Product Data Retrieval
- **File**: `CartService.java`
- **Method**: `getCartItems` (lines 25-60)
- **Code**:
```java
String sql = """
    SELECT 
        ci.cart_item_id as id,
        ci.quantity,
        p.product_id as productId,
        p.name as productName,
        p.brand,
        p.category_id as categoryId,
        p.selling_price as price,
        p.mrp,
        p.unit,
        p.image_url as imageUrl,
        ci.subtotal
    FROM cart c
    JOIN cart_items ci ON c.cart_id = ci.cart_id
    JOIN products p ON ci.product_id = p.product_id
    WHERE c.user_id = ? AND c.is_active = 1
    ORDER BY ci.added_at DESC
    """;
```
- **Join Query**: Cart items with product details

### Product API Endpoints

#### Step 1: Get All Products
- **File**: `api.js`
- **Function**: `getAllProducts` (lines 210-215)
- **Code**:
```javascript
export const getAllProducts = () => 
    axios.get(`${API_BASE_URL}/products`);
```
- **Purpose**: Retrieve complete product catalog

#### Step 2: Get Product by RFID
- **File**: `api.js`
- **Function**: `getProductByRfid` (lines 205-210)
- **Code**:
```javascript
export const getProductByRfid = (rfidTag) => 
    axios.get(`${API_BASE_URL}/products/rfid/${rfidTag}`);
```
- **Purpose**: Get product details by RFID tag

#### Step 3: Backend Product Controller
- **File**: `ProductController.java`
- **Endpoints**:
```java
@GetMapping
public ResponseEntity<?> getAllProducts() {
    List<ProductDTO> products = productService.getAllProducts();
    return ResponseEntity.ok(products);
}

@GetMapping("/rfid/{rfidTag}")
public ResponseEntity<?> getProductByRfid(@PathVariable String rfidTag) {
    ProductDTO product = productService.getProductByRfid(rfidTag);
    return ResponseEntity.ok(product);
}
```

## Database Schema

### products Table
- `product_id` (Primary Key)
- `name` (String)
- `brand` (String)
- `category_id` (Foreign Key)
- `selling_price` (Decimal)
- `mrp` (Decimal)
- `unit` (String)
- `description` (Text)
- `image_url` (String)
- `stock_quantity` (Integer)
- `is_active` (Boolean)
- `created_at`, `updated_at` (Timestamps)

### rfid_tags Table
- `rfid_tag_id` (Primary Key)
- `product_id` (Foreign Key to products)
- `rfid_tag` (String, Unique)
- `is_active` (Boolean)
- `created_at` (Timestamps)

### categories Table
- `category_id` (Primary Key)
- `name` (String)
- `description` (Text)
- `is_active` (Boolean)

## Product Data Flow

### Static Product List (Frontend)
- **Source**: Hard-coded in `ShoppingHandheld.js`
- **Purpose**: Demo product selection
- **Data**: Basic product info with RFID tags

### Dynamic Product Loading (Backend)
- **Source**: Database via API
- **Purpose**: Real product catalog
- **Data**: Complete product information

### RFID Integration
- **Mapping**: RFID tag → Product ID → Product details
- **Validation**: Active RFID tags only
- **Error Handling**: Invalid RFID tag handling

## Error Handling
- **Product Not Found**: Invalid RFID tag
- **Inactive Products**: Only active products shown
- **Stock Validation**: Future stock checking
- **Database Errors**: Connection and query failures

## Performance Optimizations
- **Indexing**: RFID tag and product ID indexes
- **Caching**: Product data caching (future)
- **Pagination**: Large catalog handling
- **Lazy Loading**: On-demand product details

## Future Enhancements
- **Dynamic Product Loading**: Replace static list with API
- **Product Search**: Search and filter functionality
- **Inventory Management**: Real-time stock tracking
- **Product Images**: Image upload and display
- **Category Management**: Hierarchical categories