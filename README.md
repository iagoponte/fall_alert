# Fall Alert (Queda Alerta) 🛡️
**Aplicativo Android para detecção de quedas em idosos via acelerômetro**

---

## Problema Real
Quedas são a principal causa de morte acidental em idosos no Brasil (DATASUS/MS).
~30% dos idosos acima de 65 anos sofrem ao menos uma queda por ano.
Muitos ficam horas no chão sem conseguir pedir socorro, especialmente os que moram sozinhos.

---

## Solução
Monitoramento contínuo do acelerômetro em segundo plano.
Quando uma queda é detectada (magnitude > 25 m/s²), o app:
1. Exibe tela de alerta com countdown de 30 segundos
2. Permite que o usuário cancele caso seja falso alarme
3. Captura localização GPS automaticamente
4. Envia SMS para todos os contatos de emergência cadastrados com link do Google Maps

---

## Sensores utilizados
- **Acelerômetro** (`TYPE_ACCELEROMETER`) — detecção principal de queda
- **GPS** via FusedLocationProviderClient — localização no momento do evento

---

## Requisitos
- Android 8.0+ (API 26)
- Android Studio Hedgehog (2023.1.1) ou superior
- Kotlin 1.9+
- Gradle 8.2+

---

## Como instalar e rodar

### Opção 1 — Android Studio
```
1. Abra o Android Studio
2. File → Open → selecione a pasta QuedaAlerta/
3. Aguarde o Gradle sync
4. Conecte um dispositivo Android real (emulador não tem acelerômetro físico)
5. Run → Run 'app'
```

### Opção 2 — Linha de comando
```bash
cd QuedaAlerta
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Permissões solicitadas
| Permissão | Finalidade |
|-----------|-----------|
| `SEND_SMS` | Envio do alerta de emergência |
| `ACCESS_FINE_LOCATION` | Coordenadas GPS no momento da queda |
| `FOREGROUND_SERVICE` | Monitoramento em segundo plano |
| `POST_NOTIFICATIONS` | Notificação persistente do serviço |
| `WAKE_LOCK` | Manter CPU ativa durante monitoramento |

---

## Arquitetura do projeto

```
app/src/main/
├── java/com/quedaalerta/
│   ├── MainActivity.kt          ← Host com BottomNavigationView
│   ├── HomeFragment.kt          ← Tela inicial / toggle de monitoramento
│   ├── SensorFragment.kt        ← Leitura em tempo real do acelerômetro
│   ├── ContactsFragment.kt      ← CRUD de contatos de emergência
│   ├── AlertActivity.kt         ← Tela de alerta com countdown 30s
│   ├── FallDetectionService.kt  ← Serviço foreground + lógica de detecção + SMS
│   └── ContactsManager.kt       ← Data class + Repository (SharedPreferences/JSON)
│
├── res/
│   ├── layout/
│   │   ├── activity_main.xml
│   │   ├── activity_alert.xml
│   │   ├── fragment_home.xml
│   │   ├── fragment_sensor.xml
│   │   ├── fragment_contacts.xml
│   │   ├── item_contact.xml
│   │   └── dialog_add_contact.xml
│   ├── menu/bottom_nav_menu.xml
│   ├── values/
│   │   ├── strings.xml
│   │   ├── colors.xml
│   │   └── themes.xml
│   └── drawable/
│       ├── ic_shield.xml
│       └── bg_avatar.xml
│
└── AndroidManifest.xml
```

---

## Lógica de detecção de queda

```
Magnitude = √(x² + y² + z²)

Em repouso:         ~9.8 m/s²  (apenas gravidade)
Movimento normal:   10–15 m/s²
Queda real:         > 25 m/s²  (impacto + variação brusca)

Debounce: mínimo 60 segundos entre alertas consecutivos
```

---

## Requisitos Funcionais
- RF01 — Monitorar acelerômetro em segundo plano continuamente
- RF02 — Detectar queda via threshold de aceleração (>25 m/s²)
- RF03 — Exibir janela de 30s para cancelar falso alarme
- RF04 — Capturar coordenadas GPS no momento do evento
- RF05 — Enviar SMS para todos os contatos cadastrados
- RF06 — Gerenciar contatos de emergência (CRUD)

## Requisitos Não Funcionais
- RNF01 — Android 8.0+ (API 26+)
- RNF02 — Interface Material Design 3
- RNF03 — Consumo de bateria otimizado (SENSOR_DELAY_NORMAL)
- RNF04 — Funciona sem internet (SMS nativo)
- RNF05 — Tempo de resposta < 500ms

---

## Possíveis evoluções futuras
- Integração com API de localização em tempo real (Firestore)
- Machine Learning para reduzir falsos positivos (TensorFlow Lite)
- Integração com wearables (Android Wear OS)
- Painel web para familiares acompanharem histórico
- Notificação por WhatsApp além de SMS
