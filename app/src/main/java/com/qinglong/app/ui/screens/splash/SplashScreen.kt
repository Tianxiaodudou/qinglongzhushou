package com.qinglong.app.ui.screens.splash

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qinglong.app.data.api.ApiManager
import com.qinglong.app.data.api.AutoLoginResult
import com.qinglong.app.util.ThemeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

data class SplashUiState(
    val isReady: Boolean = false,
    val needLogin: Boolean = false,
    val tokenFailedMessage: String? = null,
    val passwordFailedMessage: String? = null,
    val tokenSuccessMessage: String? = null,
    val passwordSuccessMessage: String? = null
)

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val apiManager: ApiManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // 设置超时，防止 tryAutoLogin 卡住（网络请求有30秒超时，这里给35秒）
            val result = withTimeoutOrNull(35_000) {
                apiManager.tryAutoLogin()
            }
            if (result == null) {
                // 超时了，跳转到登录页
                _uiState.update {
                    it.copy(
                        isReady = true,
                        needLogin = true,
                        tokenFailedMessage = "自动登录超时",
                        passwordFailedMessage = "网络请求超时，请手动登录"
                    )
                }
                return@launch
            }
            if (result.success) {
                _uiState.update {
                    it.copy(
                        isReady = true,
                        needLogin = false,
                        tokenSuccessMessage = result.tokenSuccessMessage,
                        passwordSuccessMessage = result.passwordSuccessMessage
                    )
                }
            } else {
                // 自动登录失败，需要手动登录
                _uiState.update {
                    it.copy(
                        isReady = true,
                        needLogin = true,
                        tokenFailedMessage = result.tokenFailedMessage,
                        passwordFailedMessage = result.passwordFailedMessage
                    )
                }
            }
        }
    }
}

@Composable
fun SplashScreen(
    viewModel: SplashViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    // 通过 background 颜色判断当前主题（深色/浅色）
    val bgColor = MaterialTheme.colorScheme.background
    val isDark = bgColor.red < 0.3f && bgColor.green < 0.3f && bgColor.blue < 0.3f

    // Toast 提示
    LaunchedEffect(uiState.tokenFailedMessage) {
        uiState.tokenFailedMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(uiState.passwordFailedMessage) {
        uiState.passwordFailedMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(uiState.tokenSuccessMessage) {
        uiState.tokenSuccessMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(uiState.passwordSuccessMessage) {
        uiState.passwordSuccessMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    // 无限动画（呼吸灯 + 透明度）
    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    // 透明度动画
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // 登录完成后跳转
    LaunchedEffect(uiState.isReady) {
        if (uiState.isReady) {
            // 等待动画完成一帧
            delay(500)
            if (uiState.needLogin) {
                onNavigateToLogin()
            } else {
                onNavigateToHome()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isDark) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1A2E),
                            Color(0xFF16213E)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo 图标
            Icon(
                painter = painterResource(id = com.qinglong.app.R.drawable.ic_launcher_foreground),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(80.dp)
                    .alpha(alpha)
                    .clip(RoundedCornerShape(16.dp)),
                tint = Color.Unspecified
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 应用名称
            Text(
                text = "青龙助手",
                fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 加载提示
            Text(
                text = "正在登录...",
                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 加载指示器
            LinearProgressIndicator(
                modifier = Modifier
                    .width(200.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.onPrimary,
                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)
            )
        }
    }
}
