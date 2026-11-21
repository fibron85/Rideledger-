package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Copyright
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.ui.theme.Amber500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Sky500

private val BoltGreen = Color(0xFF16A34A)
private val UberSlate = Color(0xFF334155)
private val CareemEmerald = Color(0xFF0D9488)
private val DtcSky = Color(0xFF0284C7)
private val EventViolet = Color(0xFF7C3AED)
private val InAppIndigo = Color(0xFF6366F1)

@Composable
fun sleekCardBorder(alpha: Float = 0.55f): BorderStroke =
    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = alpha))

fun paymentMethodIcon(method: String): ImageVector = when (method.trim().lowercase()) {
    "card" -> Icons.Default.CreditCard
    "inapppaid", "in-app", "inapp" -> Icons.Default.Smartphone
    "account" -> Icons.Default.AccountBalance
    else -> Icons.Default.Payments
}

fun paymentMethodTint(method: String): Color = when (method.trim().lowercase()) {
    "card" -> Sky500
    "inapppaid", "in-app", "inapp" -> InAppIndigo
    "account" -> EventViolet
    else -> Emerald500
}

@Composable
fun PaymentMethodSymbolBadge(
    paymentMethod: String,
    modifier: Modifier = Modifier
) {
    val resolved = paymentMethod.ifBlank { "Cash" }
    val tint = paymentMethodTint(resolved)
    val icon = paymentMethodIcon(resolved)
    Surface(
        color = tint.copy(alpha = 0.14f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.28f)),
        modifier = modifier
            .size(28.dp)
            .semantics { contentDescription = resolved }
            .testTag("payment_symbol_${resolved.lowercase()}")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = resolved,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun PlatformPillBadge(
    platformName: String,
    modifier: Modifier = Modifier
) {
    val accent = when (platformName.trim().lowercase()) {
        "bolt" -> BoltGreen
        "dtc" -> DtcSky
        "dtc event booking" -> EventViolet
        "careem" -> CareemEmerald
        "uber" -> UberSlate
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(
        color = accent.copy(alpha = 0.14f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
        modifier = modifier
    ) {
        Text(
            text = platformName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun SettledBadge(
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Emerald500.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.3f)),
        modifier = modifier.semantics { contentDescription = "Settled to Finance" }
    ) {
        if (compact) {
            Box(
                modifier = Modifier.size(26.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Settled",
                    tint = Emerald500,
                    modifier = Modifier.size(15.dp)
                )
            }
        } else {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Emerald500,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Settled",
                    style = MaterialTheme.typography.labelSmall,
                    color = Emerald500,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TipPillBadge(
    amountText: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Amber500.copy(alpha = 0.14f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Amber500.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Text(
            text = "+Tip $amountText",
            style = MaterialTheme.typography.labelSmall,
            color = Amber500,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutAppSectionCard(
    modifier: Modifier = Modifier,
    testTag: String = "settings_about_section"
) {
    val context = LocalContext.current
    val aboutTitle = stringResource(R.string.about_section_title)
    val appName = stringResource(R.string.app_name)
    val versionLabel = "App Version ${BuildConfig.VERSION_NAME}"
    val appDescription = stringResource(R.string.about_app_description)
    val copyrightNotice = stringResource(R.string.copyright_notice)
    val developerCredit = stringResource(R.string.developer_credit)
    val email = stringResource(R.string.developer_email)
    val mobile = stringResource(R.string.developer_mobile)

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(aboutTitle, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = appName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = versionLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = appDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium,
                border = sleekCardBorder(0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Copyright,
                            contentDescription = "Copyright",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = copyrightNotice,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                    Text(
                        text = developerCredit,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    runCatching {
                                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("mailto:$email")
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Email: $email",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    runCatching {
                                        val intent = Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:$mobile")
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Mobile",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Mobile: $mobile",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
