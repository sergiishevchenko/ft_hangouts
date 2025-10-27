package com.example.ft_hangouts_42.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.os.Build
import com.example.ft_hangouts_42.data.ContactRepository
import com.example.ft_hangouts_42.data.MessageRepository
import com.example.ft_hangouts_42.data.room.ContactEntity
import com.example.ft_hangouts_42.data.room.MessageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SMSReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {
            val bundle = intent.extras
            if (bundle != null) {
                val pdus = bundle.get("pdus") as? Array<*> ?: return
                val format = bundle.getString("format") ?: return

                for (pdu in pdus) {
                    val sms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        SmsMessage.createFromPdu(pdu as ByteArray, format)
                    } else {
                        SmsMessage.createFromPdu(pdu as ByteArray)
                    } ?: continue

                    val address = sms.displayOriginatingAddress ?: continue
                    val body = sms.messageBody ?: continue

                    CoroutineScope(Dispatchers.IO).launch {
                        val contactRepo = ContactRepository(context)
                        val messageRepo = MessageRepository(context)

                        var contact = contactRepo.getByPhone(address)

                        if (contact == null) {
                            val newContact = ContactEntity(
                                id = 0,
                                name = address,
                                phone = address,
                                email = null,
                                address = null,
                                notes = "Auto-created from SMS",
                                avatarPath = null
                            )
                            contactRepo.add(newContact)

                            contact = contactRepo.getByPhone(address)
                        }

                        contact?.let {
                            val message = MessageEntity(
                                contactId = it.id,
                                text = body,
                                timestamp = System.currentTimeMillis(),
                                isSent = false
                            )
                            messageRepo.addMessage(message)
                        }
                    }
                }
            }
        }
    }
}