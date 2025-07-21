import firebase_admin
from firebase_admin import credentials, firestore

# Thay đường dẫn này bằng đường dẫn tới file serviceAccountKey.json của bạn
cred = credentials.Certificate('D:/Androi_Studio/Final_Project/tradeupapp-5b0c3-firebase-adminsdk-fbsvc-693e37166b.json')
firebase_admin.initialize_app(cred)
db = firestore.client()

# 1. Thêm item mẫu
item_ref = db.collection('items').document()
item_ref.set({
    "title": "Laptop Dell",
    "description": "Laptop Dell cũ, còn bảo hành",
    "imageUrl": "https://via.placeholder.com/300x200.png?text=Laptop+Dell",
    "sellerId": "user1",
    "price": 500.0,
    "category": "Electronics",
    "condition": "Used",
    "location": "Hà Nội",
    "photoUrls": ["https://via.placeholder.com/300x200.png?text=Laptop+Dell"],
    "status": "Available",
    "views": 10,
    "interactions": 2
})
item_id = item_ref.id

# 2. Thêm user mẫu
user_ref = db.collection('users').document('user1')
user_ref.set({
    "uid": "user1",
    "displayName": "Nguyễn Văn A",
    "bio": "Chuyên bán đồ điện tử",
    "contactInfo": "0123456789",
    "profilePicUrl": "https://via.placeholder.com/100x100.png?text=Avatar",
    "rating": 4.5,
    "totalTransactions": 10,
    "blockedUsers": [],
    "active": True
})

user_ref2 = db.collection('users').document('user2')
user_ref2.set({
    "uid": "user2",
    "displayName": "Trần Thị B",
    "bio": "Khách hàng thân thiện",
    "contactInfo": "0987654321",
    "profilePicUrl": "https://via.placeholder.com/100x100.png?text=Avatar",
    "rating": 5.0,
    "totalTransactions": 2,
    "blockedUsers": [],
    "active": True
})

# 3. Thêm review mẫu
review_ref = db.collection('reviews').document()
review_ref.set({
    "toUserUid": "user1",
    "fromUserUid": "user2",
    "rating": 5,
    "review": "Giao dịch tốt, uy tín!",
    "timestamp": 1620000000000
})

# 4. Thêm offer mẫu
offer_ref = db.collection('offers').document()
offer_ref.set({
    "itemId": item_id,
    "sellerUid": "user1",
    "buyerUid": "user2",
    "offerPrice": 480.0,
    "status": "pending",
    "counterPrice": 0,
    "timestamp": 1620000000000
})

# 5. Thêm payment mẫu
payment_ref = db.collection('payments').document()
payment_ref.set({
    "paymentId": payment_ref.id,
    "userUid": "user2",
    "itemId": item_id,
    "amount": 480.0,
    "method": "Credit Card",
    "timestamp": 1620000000000
})

# 6. Thêm report mẫu
report_ref = db.collection('reports').document()
report_ref.set({
    "reporterId": "user2",
    "targetId": item_id,
    "type": "listing",
    "reason": "Scam/Fraud",
    "timestamp": 1620000000000
})

# 7. Thêm chat mẫu
chat_ref = db.collection('chats').document()
chat_ref.set({
    "chatId": chat_ref.id,
    "user1": "user1",
    "user2": "user2",
    "messages": [
        {
            "sender": "user1",
            "text": "Chào bạn, sản phẩm còn không?",
            "timestamp": 1620000000000
        },
        {
            "sender": "user2",
            "text": "Còn nhé!",
            "timestamp": 1620000001000
        }
    ]
})

print("Đã import dữ liệu mẫu thành công!")

