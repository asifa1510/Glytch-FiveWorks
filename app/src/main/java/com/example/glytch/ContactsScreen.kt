package com.example.glytch

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.database.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    navController: NavController,
    userEmail: String
) {
    val context = LocalContext.current

    // email → Firebase key-safe: replace "." with ","
    val safeEmail = userEmail.replace(".", ",")
    val databaseRef = remember {
        FirebaseDatabase.getInstance()
            .getReference("users")
            .child(safeEmail)
            .child("contacts")
    }

    val contacts = remember { mutableStateListOf<Contact>() }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var firebaseError by remember { mutableStateOf<String?>(null) }

    // 🔄 Listen for changes from Firebase
    LaunchedEffect(Unit) {
        try {
            databaseRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    contacts.clear()
                    for (child in snapshot.children) {
                        val contact = child.getValue(Contact::class.java)
                        // make sure id is set
                        if (contact != null) {
                            contact.id = child.key
                            contacts.add(contact)
                        }
                    }
                    firebaseError = null
                }

                override fun onCancelled(error: DatabaseError) {
                    firebaseError = "Firebase error: ${error.message}"
                }
            })
        } catch (e: Exception) {
            firebaseError = "Firebase not connected: ${e.localizedMessage}"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Emergency Contacts",
                        color = Color(0xFF4B3869),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White.copy(alpha = 0.9f)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFE0F7FA), Color(0xFFFCE4EC))
                    )
                )
                .padding(paddingValues)
                .padding(16.dp)
        ) {

            if (firebaseError != null) {
                Text(
                    text = firebaseError ?: "",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 🔤 Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name", color = Color.Gray) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF81C784),
                    unfocusedBorderColor = Color(0xFFD3D3D3),
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    cursorColor = Color(0xFF4CAF50)
                )
            )

            // 📞 Phone
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone", color = Color.Gray) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF81C784),
                    unfocusedBorderColor = Color(0xFFD3D3D3),
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    cursorColor = Color(0xFF4CAF50)
                )
            )

            // ✉️ Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", color = Color.Gray) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF81C784),
                    unfocusedBorderColor = Color(0xFFD3D3D3),
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    cursorColor = Color(0xFF4CAF50)
                )
            )

            // ➕ Add Contact
            Button(
                onClick = {
                    if (name.isBlank() || phone.isBlank()) {
                        firebaseError = "Name and phone are required."
                        return@Button
                    }
                    val key = databaseRef.push().key
                    if (key == null) {
                        firebaseError = "Failed to generate contact key."
                        return@Button
                    }
                    val contact = Contact(
                        id = key,
                        name = name.trim(),
                        phone = phone.trim(),
                        email = email.trim()
                    )
                    databaseRef.child(key).setValue(contact)
                    name = ""
                    phone = ""
                    email = ""
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFA8E6CF)
                )
            ) {
                Text("➕ Add Emergency Contact", color = Color.Black)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // List of contacts from Firebase
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 4.dp)
            ) {
                items(contacts) { contact ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .animateContentSize(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFDFDFD)
                        ),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    contact.name ?: "",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.Black
                                )
                                Text(
                                    contact.phone ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.DarkGray
                                )
                                Text(
                                    contact.email ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                            Button(
                                onClick = {
                                    contact.id?.let { id ->
                                        databaseRef.child(id).removeValue()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFB6C1)
                                )
                            ) {
                                Text("Delete", color = Color.Black)
                            }
                        }
                    }
                }
            }

            // Back button to go home
            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD6E0)
                )
            ) {
                Text("⬅ Back", color = Color.Black)
            }
        }
    }
}
