package com.example.myapplication.Fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.myapplication.activities.AddActivity
import com.example.myapplication.databinding.FragmentScanQRCodeBinding
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult

class ScanQRCodeFragment : Fragment() {

    private lateinit var binding: FragmentScanQRCodeBinding
    private lateinit var decoratedBarcodeView: DecoratedBarcodeView
    private lateinit var captureManager: CaptureManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentScanQRCodeBinding.inflate(layoutInflater, container, false)
        decoratedBarcodeView = binding.barcodeScannerView
        captureManager = CaptureManager(requireActivity(), decoratedBarcodeView)
        captureManager.initializeFromIntent(requireActivity().intent, savedInstanceState)
        captureManager.decode()


        decoratedBarcodeView.barcodeView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                // Handle the scanned result here
                val scannedData = result.text
                //reset trang thai camera
                decoratedBarcodeView.barcodeView.resume()
                // Chuyển dữ liệu quét được
                val intent = Intent(activity, AddActivity::class.java)
                intent.putExtra("scannedData", scannedData)
                startActivity(intent)
            }

            override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {
                // Handle possible result points here (optional)
            }
        })

        decoratedBarcodeView.setStatusText("Để mã vào ô ở giữa")

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        captureManager.onResume()
    }

    override fun onPause() {
        super.onPause()
        captureManager.onPause()
    }
}