# WearOS Battery Level Check

Projeto Android composto por dois módulos que se comunicam via **Wearable Data Layer API**:

- **`:app`** — app no smartphone que exibe o nível de bateria do relógio WearOS
- **`:wear`** — app no relógio WearOS que exibe o nível de bateria do smartphone

---

## Funcionalidades

### App Smartphone (`:app`)
- Exibe o nível de bateria do relógio conectado
- Barra de progresso com cor dinâmica:
  - Vermelho `< 20%`
  - Amarelo `20% – 50%`
  - Verde `> 50%`
- Atualização automática a cada **30 segundos**
- Atualização silenciosa ao retornar ao app (`onResume`)
- Botão "Atualizar" para atualização manual com indicador de loading
- Envia a bateria do próprio smartphone ao relógio sempre que o nível muda

### App Relógio (`:wear`)
- Exibe o nível de bateria do smartphone conectado
- Indicador circular ao redor do conteúdo com cor dinâmica:
  - Vermelho `< 20%`
  - Amarelo `20% – 50%`
  - Verde `> 50%`
- Status de carregamento:
  - `⚡ Carregando` em amarelo enquanto carrega
  - `Carregado` em verde ao atingir 100%
- Solicita a bateria do smartphone ao iniciar
- Exibe hora atual via `TimeText` (padrão WearOS)

---

## Arquitetura e Comunicação

A comunicação entre os dois apps usa a **Wearable MessageClient**:

```
Smartphone (:app)                        Relógio (:wear)
─────────────────                        ───────────────────────
PhoneBatteryListenerService  ←──/request_phone_battery──  MainActivity (onCreate)
        │
        └──/phone_battery──────────────► BatteryListenerService
                                                  │
                                         phoneBattery StateFlow
                                                  │
                                          BatteryScreen (UI)

MainActivity (onBatteryChanged) ──/phone_battery──────────► BatteryListenerService

BatteryViewModel ──/battery_request──────────────────────► BatteryListenerService
                 ◄──/battery_response────────────────────
```

### Paths de mensagem

| Path | Direção | Descrição |
|---|---|---|
| `/request_phone_battery` | Relógio → Celular | Relógio solicita a bateria do celular |
| `/phone_battery` | Celular → Relógio | Celular envia sua bateria (`"75,false"`) |
| `/battery_request` | Celular → Relógio | Celular solicita a bateria do relógio |
| `/battery_response` | Relógio → Celular | Relógio responde com sua bateria (`"85,true"`) |

### Formato da mensagem
```
"<level>,<isCharging>"
// Exemplos:
"75,false"   // 75%, não carregando
"100,true"   // 100%, carregando
```

---

## Estrutura do Projeto

```
WearOsBatteryLevelCheck/
├── app/                                  # Módulo smartphone
│   └── src/main/java/.../
│       ├── MainActivity.kt               # Envia bateria ao relógio via BroadcastReceiver
│       ├── BatteryViewModel.kt           # Lógica de estado + refresh periódico
│       ├── BatteryScreen.kt              # Tela Compose (Material3)
│       └── PhoneBatteryListenerService.kt# Responde requisições do relógio
│
├── wear/                                 # Módulo relógio
│   └── src/main/java/.../
│       ├── MainActivity.kt               # Solicita bateria do celular ao iniciar
│       ├── BatteryScreen.kt              # Tela Wear Compose
│       └── BatteryListenerService.kt     # Recebe bateria do celular + responde bateria do relógio
│
└── gradle/libs.versions.toml             # Catálogo de dependências
```

---

## Tecnologias

| Tecnologia | Versão |
|---|---|
| Android Gradle Plugin | 9.0.1 |
| Kotlin | 2.0.21 |
| Compose BOM | 2024.09.00 |
| Wear Compose Material | 1.4.1 |
| Play Services Wearable | 18.2.0 |
| Lifecycle | 2.10.0 |
| Min SDK | 30 (Android 11 / WearOS 3.0) |
| Target SDK | 36 |

---

## Requisitos

- Smartphone Android com o app `:app` instalado
- Relógio WearOS 3.0+ com o app `:wear` instalado
- Ambos os apps devem ter o mesmo `applicationId`: `com.leandrocaf.wearosbatterylevelcheck`
- O relógio deve estar pareado e conectado ao smartphone via Bluetooth

---

## Como executar

1. Abra o projeto no **Android Studio**
2. Conecte o smartphone via USB (ou use emulador com API 30+)
3. Conecte o relógio WearOS via ADB sobre Bluetooth ou emulador Wear
4. Execute o módulo `:app` no smartphone
5. Execute o módulo `:wear` no relógio
