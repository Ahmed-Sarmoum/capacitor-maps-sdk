// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapacitorMapsSdk",
    platforms: [.iOS(.v14)],
    products: [
        .library(
            name: "CapacitorMapsSdk",
            targets: ["CapacitorMapSdkPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "7.0.0")
    ],
    targets: [
        .target(
            name: "CapacitorMapSdkPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/CapacitorMapSdkPlugin"),
        .testTarget(
            name: "CapacitorMapSdkPluginTests",
            dependencies: ["CapacitorMapSdkPlugin"],
            path: "ios/Tests/CapacitorMapSdkPluginTests")
    ]
)