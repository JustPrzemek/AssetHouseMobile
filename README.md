# AssetHouse MOBILE

Krótki opis aplikacji – aplikacja integrujaca skaner rfid zebra MC3300U, sluzaca do skanowania tagów RFID.

Wymagane uprawnienia: narazie brak

Do uruchomienia z CMD:

Zainstalowany Android SDK (ANDROID_HOME ze sciezka w zmiennej srodowiskowej PATH).
	
Narzedzie adb (Android Debug Bridge) - to tylko sluzy do uruchomienia aplikacji i laczenia z urzadzeniem na ktorym aplikacja zostala uruchomiona, apk mozna bez tego uzadzenia zbudowac.

# Uruchomienie projektu
# Opcja 1: Uruchamianie w Android Studio
1. Sklonuj repozytorium
git clone //url
	
2. Otwórz projekt w Android Studio
- Uruchom Android Studio i wybierz "Open".

- Wskaz folder sklonowanego projektu.

3. Zainstaluj zaleznosci
- Android Studio powinien automatycznie wykryc i zsynchronizowac zaleznosci Gradle.

- Jesli wystepuja bledy, uzyj opcji:
		File -> Sync Project with Gradle Files.

4. Podlacz urzadzenie lub emulator
- Wlacz tryb deweloperski na telefonie:
	Settings -> About Phone -> kliknij 7x w "Build Number".
	W Developer Options wlacz USB Debugging.

- Podlacz telefon przez USB lub uruchom emulator z AVD Manager.

5. Zbuduj i uruchom aplikacje
- Kliknij "Run" (zielona strzalka) lub uzyj skrótu Shift + F10.



# Opcja 2: Uruchamianie z CMD (Windows) bez Android Studio
1. Sklonuj repozytorium

git clone //url

cd nazwa-repo

2. Sprawdz, czy masz zainstalowane Android SDK i adb (adb sluzy do zarzadzania aplikacja, apk mozna zbudowac bez tego patrz punkt 3)

adb --version  # Sprawdz, czy adb jest dostepne
	
Jesli nie masz:
- Pobierz Android SDK Tools i dodaj sciezke do adb (np. C:\Android\platform-tools) do zmiennej srodowiskowej PATH.

3. Zbuduj APK (debug) za pomoca Gradle

.\gradlew assembleDebug  # Windows

Lub dla Linux/Mac:

./gradlew assembleDebug

Gotowy plik APK znajdziesz w:

app/build/outputs/apk/debug/app-debug.apk.

4. Zainstaluj APK na urzadzeniu

adb install app/build/outputs/apk/debug/app-debug.apk

5. Uruchom aplikacje

adb shell am start -n com.example.pakiet/.MainActivity
(Zastap com.example.pakiet swoim pakietem z AndroidManifest.xml)

# Konfiguracja (opcjonalne)
Jesli aplikacja wymaga kluczy API lub plików konfiguracyjnych, proxy, dodaj je np. w local.properties:

api.key=twoj_klucz_tutaj 
