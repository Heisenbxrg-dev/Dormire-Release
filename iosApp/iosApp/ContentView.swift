import SwiftUI

struct ContentView: View {
    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            VStack(spacing: 12) {
                Text("Dormire iOS")
                    .font(.largeTitle.bold())
                    .foregroundColor(.white)
                Text("Build KMP pronta per l'integrazione Compose")
                    .font(.subheadline)
                    .foregroundColor(.gray)
            }
        }
    }
}
