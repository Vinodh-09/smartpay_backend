# Shopping Cart Management Functionality

## Overview
The shopping cart system manages product addition, quantity updates, and cart persistence during the shopping session. It supports both RFID-based automatic product addition and manual product selection.

## Involved Files

### Backend Files
1. **CartController.java** - REST API endpoints for cart operations
2. **CartService.java** - Business logic for cart management
3. **Product.java** - Product entity
4. **ProductRepository.java** - Product data access
5. **User.java** - User entity
6. **UserRepository.java** - User data access

### Frontend Files
1. **ShoppingHandheld.js** - Main shopping interface with cart display
2. **CartReview.js** - Cart review and modification screen
3. **api.js** - API client functions for cart operations

### Database Tables
- **cart**: Shopping cart sessions
- **cart_items**: Items in shopping carts
- **products**: Product catalog
- **rfid_tags**: RFID tag to product mapping
- **users**: User accounts

## Process Flow

### RFID-Based Product Addition

#### Step 1: RFID Tag Detection
- **File**: `ShoppingHandheld.js`
- **Function**: `handleRefreshCart` (lines 305-315)
- **Code**:
```javascript
const handleRefreshCart = useCallback(async () => {
  try {
    const cartResponse = await getCart(userId);
    const totalResponse = await getCartTotal(userId);
    onUpdateCart(cartResponse.data, totalResponse.data);
  } catch (err) {
    setError('Failed to refresh cart.');
  }
}, [cart, userId, onUpdateCart]);
```
- **Trigger**: Called when RFID scanner detects new product

#### Step 2: Backend RFID Processing
- **File**: `CartController.java`
- **Endpoint**: `POST /api/cart/add-by-rfid`
- **Method**: `addItemByRfid` (lines 75-85)
- **Code**:
```java
@PostMapping("/add-by-rfid")
public ResponseEntity<?> addItemByRfid(@RequestBody Map<String, Object> request) {
    Long userId = Long.valueOf(request.get("userId").toString());
    String rfidTag = request.get("rfidTag").toString());
    
    cartService.addItemByRfid(userId, rfidTag);
    return ResponseEntity.ok(Map.of("success", true, "message", "Item added to cart"));
}
```
- **Request Body**: `{"userId": 1, "rfidTag": "RFID0001"}`

#### Step 3: Cart Service RFID Logic
- **File**: `CartService.java`
- **Method**: `addItemByRfid` (lines 188-252)
- **Code**:
```java
@Transactional
public void addItemByRfid(Long userId, String rfidTag) {
    // First, get product ID from RFID tag
    String productSql = "SELECT product_id FROM rfid_tags WHERE rfid_tag = ? AND is_active = 1";
    Long productId = jdbcTemplate.queryForObject(productSql, Long.class, rfidTag);
    
    if (productId == null) {
        throw new IllegalArgumentException("Product not found for RFID tag: " + rfidTag);
    }
    
    // Get or create cart for user
    String getCartIdSql = "SELECT cart_id FROM cart WHERE user_id = ? AND is_active = 1";
    List<Long> cartIds = jdbcTemplate.query(getCartIdSql, (rs, rowNum) -> rs.getLong("cart_id"), userId);
    
    Long cartId;
    if (cartIds.isEmpty()) {
        // Create new cart
        String insertCartSql = "INSERT INTO cart (user_id, is_active, created_at, updated_at) VALUES (?, 1, NOW(), NOW())";
        jdbcTemplate.update(insertCartSql, userId);
        cartId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    } else {
        cartId = cartIds.get(0);
    }
    
    // Get product price
    String priceSql = "SELECT selling_price FROM products WHERE product_id = ?";
    BigDecimal price = jdbcTemplate.queryForObject(priceSql, BigDecimal.class, productId);
    
    // Check if item already exists in cart
    String checkSql = "SELECT cart_item_id, quantity FROM cart_items WHERE cart_id = ? AND product_id = ?";
    List<Map<String, Object>> existingItems = jdbcTemplate.query(checkSql, (rs, rowNum) -> {
        Map<String, Object> item = new HashMap<>();
        item.put("cartItemId", rs.getLong("cart_item_id"));
        item.put("quantity", rs.getInt("quantity"));
        return item;
    }, cartId, productId);
    
    if (!existingItems.isEmpty()) {
        // Update existing item quantity
        Long cartItemId = (Long) existingItems.get(0).get("cartItemId");
        int newQuantity = (Integer) existingItems.get(0).get("quantity") + 1;
        
        String updateSql = """
            UPDATE cart_items 
            SET quantity = ?, subtotal = ? * ?, updated_at = NOW()
            WHERE cart_item_id = ?
            """;
        jdbcTemplate.update(updateSql, newQuantity, price, newQuantity, cartItemId);
    } else {
        // Add new item to cart
        String insertSql = """
            INSERT INTO cart_items (cart_id, product_id, quantity, unit_price, subtotal, added_at, updated_at)
            VALUES (?, ?, 1, ?, ?, NOW(), NOW())
            """;
        jdbcTemplate.update(insertSql, cartId, productId, price, price);
    }
}
```
- **Database Operations**: RFID lookup, cart creation/retrieval, item addition/update

### Manual Product Addition

#### Step 1: Product Selection
- **File**: `ShoppingHandheld.js`
- **Function**: `handleAddManualProduct` (lines 318-332)
- **Code**:
```javascript
const handleAddManualProduct = useCallback(async (product) => {
  try {
    setLoading(true);
    setError('');
    
    // Call backend API to add item using RFID tag
    await addItemsToCart(userId, product.rfidTag);
    
    // Refresh cart after adding
    const cartResponse = await getCart(userId);
    const totalResponse = await getCartTotal(userId);
    onUpdateCart(cartResponse.data, totalResponse.data);
    
    setLoading(false);
  } catch (err) {
    setLoading(false);
    setError(`Failed to add ${product.name}: ${err.response?.data?.message || err.message}`);
  }
}, [userId, onUpdateCart]);
```
- **UI**: Product list with "Add to Cart" buttons

#### Step 2: API Call
- **File**: `api.js`
- **Function**: `addItemsToCart` (lines 199-201)
- **Code**:
```javascript
export const addItemsToCart = (userId, rfidTag) => 
    axios.post(`${API_BASE_URL}/cart/add-by-rfid`, { userId, rfidTag });
```
- **Same Endpoint**: Uses same RFID endpoint for consistency

### Cart Quantity Management

#### Step 1: Quantity Change UI
- **File**: `ShoppingHandheld.js`
- **Function**: `handleQuantityChange` (lines 250-275)
- **Code**:
```javascript
const handleQuantityChange = useCallback(async (cartItemId, newQuantity) => {
  if (newQuantity < 1) return;
  
  // Optimistically update UI immediately for instant feedback
  const updatedCart = cart.map(item =>
    item.id === cartItemId ? { ...item, quantity: newQuantity } : item
  );
  const newTotal = updatedCart.reduce((sum, item) => sum + (item.item.price * item.quantity), 0);
  onUpdateCart(updatedCart, newTotal);
  
  // Then sync with backend silently
  try {
    await updateCartItem(userId, cartItemId, newQuantity);
    setError('');
  } catch (err) {
    setError('Failed to update quantity.');
    // Rollback on error
    const cartResponse = await getCart(userId);
    const totalResponse = await getCartTotal(userId);
    onUpdateCart(cartResponse.data, totalResponse.data);
  }
}, [cart, userId, onUpdateCart]);
```
- **Optimistic Updates**: UI updates immediately, backend syncs in background

#### Step 2: Backend Quantity Update
- **File**: `CartController.java`
- **Endpoint**: `PUT /api/cart/{userId}/item/{cartItemId}`
- **Method**: `updateCartItem` (lines 35-42)
- **Code**:
```java
@PutMapping("/{userId}/item/{cartItemId}")
public ResponseEntity<?> updateCartItem(
        @PathVariable Long userId,
        @PathVariable Long cartItemId,
        @RequestBody Map<String, Integer> request) {
    int quantity = request.get("quantity");
    cartService.updateCartItemQuantity(cartItemId, quantity);
    return ResponseEntity.ok(Map.of("success", true, "message", "Cart item updated"));
}
```

#### Step 3: Service Quantity Logic
- **File**: `CartService.java`
- **Method**: `updateCartItemQuantity` (lines 150-165)
- **Code**:
```java
@Transactional
public void updateCartItemQuantity(Long cartItemId, int quantity) {
    // Get current item details
    String selectSql = "SELECT unit_price FROM cart_items WHERE cart_item_id = ?";
    BigDecimal unitPrice = jdbcTemplate.queryForObject(selectSql, BigDecimal.class, cartItemId);
    
    // Calculate new subtotal
    BigDecimal subtotal = unitPrice.multiply(new BigDecimal(quantity));
    
    // Update quantity and subtotal
    String updateSql = """
        UPDATE cart_items 
        SET quantity = ?, subtotal = ?, updated_at = NOW()
        WHERE cart_item_id = ?
        """;
    jdbcTemplate.update(updateSql, quantity, subtotal, cartItemId);
}
```
- **Subtotal Calculation**: Updates both quantity and calculated subtotal

### Cart Item Removal

#### Step 1: Remove Action
- **File**: `ShoppingHandheld.js`
- **Function**: `handleRemoveItem` (lines 277-295)
- **Code**:
```javascript
const handleRemoveItem = useCallback(async (cartItemId) => {
  // Optimistically update UI immediately
  const updatedCart = cart.filter(item => item.id !== cartItemId);
  const newTotal = updatedCart.reduce((sum, item) => sum + (item.item.price * item.quantity), 0);
  onUpdateCart(updatedCart, newTotal);
  
  // Then sync with backend silently
  try {
    await removeCartItem(userId, cartItemId);
    setError('');
  } catch (err) {
    setError('Failed to remove item.');
    // Rollback on error
    const cartResponse = await getCart(userId);
    const totalResponse = await getCartTotal(userId);
    onUpdateCart(cartResponse.data, totalResponse.data);
  }
}, [cart, userId, onUpdateCart]);
```

#### Step 2: Backend Removal
- **File**: `CartController.java`
- **Endpoint**: `DELETE /api/cart/{userId}/item/{cartItemId}`
- **Method**: `removeCartItem` (lines 44-49)
- **Code**:
```java
@DeleteMapping("/{userId}/item/{cartItemId}")
public ResponseEntity<?> removeCartItem(
        @PathVariable Long userId,
        @PathVariable Long cartItemId) {
    cartService.removeCartItem(cartItemId);
    return ResponseEntity.ok(Map.of("success", true, "message", "Item removed from cart"));
}
```

#### Step 3: Service Removal Logic
- **File**: `CartService.java`
- **Method**: `removeCartItem` (lines 168-180)
- **Code**:
```java
@Transactional
public void removeCartItem(Long cartItemId) {
    String sql = "DELETE FROM cart_items WHERE cart_item_id = ?";
    int rowsAffected = jdbcTemplate.update(sql, cartItemId);
    
    if (rowsAffected == 0) {
        throw new IllegalArgumentException("Cart item not found: " + cartItemId);
    }
}
```

### Cart Total Calculation

#### Step 1: Get Cart Total API
- **File**: `CartController.java`
- **Endpoint**: `GET /api/cart/{userId}/total`
- **Method**: `getCartTotal` (lines 20-25)
- **Code**:
```java
@GetMapping("/{userId}/total")
public ResponseEntity<?> getCartTotal(@PathVariable Long userId) {
    Map<String, Object> total = cartService.getCartTotal(userId);
    return ResponseEntity.ok(total);
}
```

#### Step 2: Service Total Calculation
- **File**: `CartService.java`
- **Method**: `getCartTotal` (lines 75-105)
- **Code**:
```java
public Map<String, Object> getCartTotal(Long userId) {
    String sql = """
        SELECT 
            COALESCE(SUM(ci.subtotal), 0) as subtotal,
            COUNT(ci.cart_item_id) as itemCount
        FROM cart c
        JOIN cart_items ci ON c.cart_id = ci.cart_id
        WHERE c.user_id = ? AND c.is_active = 1
        """;
    
    return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
        BigDecimal subtotal = rs.getBigDecimal("subtotal");
        int itemCount = rs.getInt("itemCount");
        
        // Prices are inclusive of tax - no additional tax calculation
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal total = subtotal;
        
        Map<String, Object> result = new HashMap<>();
        result.put("subtotal", subtotal);
        result.put("discount", BigDecimal.ZERO);
        result.put("tax", tax);
        result.put("total", total);
        result.put("itemCount", itemCount);
        
        return result;
    }, userId);
}
```
- **Total Calculation**: Sum of all cart item subtotals

## API Endpoints

### Cart Operations
- **GET /api/cart/{userId}**: Get cart items
- **GET /api/cart/{userId}/total**: Get cart total
- **POST /api/cart/add-by-rfid**: Add item by RFID
- **PUT /api/cart/{userId}/item/{cartItemId}**: Update item quantity
- **DELETE /api/cart/{userId}/item/{cartItemId}**: Remove item
- **DELETE /api/cart/{userId}**: Clear cart

## Database Schema

### cart Table
- `cart_id` (Primary Key)
- `user_id` (Foreign Key to users)
- `is_active` (Boolean)
- `created_at`, `updated_at` (Timestamps)

### cart_items Table
- `cart_item_id` (Primary Key)
- `cart_id` (Foreign Key to cart)
- `product_id` (Foreign Key to products)
- `quantity` (Integer)
- `unit_price` (Decimal)
- `subtotal` (Decimal)
- `added_at`, `updated_at` (Timestamps)

## Error Handling
- **Product Not Found**: RFID tag not associated with product
- **Cart Item Not Found**: Invalid cart item ID
- **Optimistic Update Failures**: Rollback to server state
- **Database Constraint Violations**: Proper transaction management

## Performance Optimizations
- **Optimistic UI Updates**: Instant feedback without waiting for backend
- **Background Sync**: Non-blocking backend operations
- **Database Indexing**: Optimized queries on user_id and cart_id
- **Connection Pooling**: Efficient database connections