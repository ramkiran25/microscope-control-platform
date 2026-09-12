from PySide6.QtWidgets import (
    QWidget,
    QVBoxLayout,
    QLabel,
    QPushButton,
    QTextEdit,
)

from api_client import MicroscopeApiClient


class MainWindow(QWidget):

    def __init__(self):
        super().__init__()

        self.api = MicroscopeApiClient()

        self.setWindowTitle("Microscope Engineering Console")
        self.resize(500, 400)

        layout = QVBoxLayout()

        title = QLabel("Microscope Engineering Console")
        title.setStyleSheet("font-size: 20px; font-weight: bold;")

        self.status_label = QLabel("Status: Ready")

        self.z_stack_button = QPushButton("Run Z-Stack")
        self.reset_button = QPushButton("Reset Instrument")
        self.start_button = QPushButton("Start Continuous")
        self.stop_button = QPushButton("Stop Continuous")

        self.log = QTextEdit()
        self.log.setReadOnly(True)

        layout.addWidget(title)
        layout.addWidget(self.status_label)

        layout.addWidget(self.z_stack_button)
        layout.addWidget(self.reset_button)
        layout.addWidget(self.start_button)
        layout.addWidget(self.stop_button)

        layout.addWidget(QLabel("Backend response:"))
        layout.addWidget(self.log)

        self.setLayout(layout)

        self.z_stack_button.clicked.connect(self.run_z_stack)
        self.reset_button.clicked.connect(self.reset)
        self.start_button.clicked.connect(self.start_continuous)
        self.stop_button.clicked.connect(self.stop_continuous)

    def run_z_stack(self):
        self.status_label.setText("Status: Running Z-Stack...")

        try:
            result = self.api.run_z_stack()

            self.log.append(result)
            self.status_label.setText("Status: Z-Stack completed")

        except Exception as e:
            self.log.append(f"ERROR: {e}")
            self.status_label.setText("Status: Error")

    def reset(self):
        try:
            result = self.api.reset()

            self.log.append(result)
            self.status_label.setText("Status: IDLE")

        except Exception as e:
            self.log.append(f"ERROR: {e}")

    def start_continuous(self):
        try:
            result = self.api.start_continuous()

            self.log.append(result)
            self.status_label.setText("Status: Continuous run started")

        except Exception as e:
            self.log.append(f"ERROR: {e}")

    def stop_continuous(self):
        try:
            result = self.api.stop_continuous()

            self.log.append(result)
            self.status_label.setText("Status: Continuous run stopped")

        except Exception as e:
            self.log.append(f"ERROR: {e}")