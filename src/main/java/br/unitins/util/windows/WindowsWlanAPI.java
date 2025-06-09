package br.unitins.util.windows;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import java.util.Arrays;
import java.util.List;

public interface WindowsWlanAPI extends Library {
    WindowsWlanAPI INSTANCE = Native.load("wlanapi", WindowsWlanAPI.class);
    
    // Constantes
    int ERROR_SUCCESS = 0;
    int WLAN_CLIENT_VERSION_2 = 2;
    int WLAN_AVAILABLE_NETWORK_INCLUDE_ALL_ADHOC_PROFILES = 0x00000001;
    int WLAN_AVAILABLE_NETWORK_INCLUDE_ALL_MANUAL_HIDDEN_PROFILES = 0x00000002;
    
    // Funções da API
    int WlanOpenHandle(
        int dwClientVersion,
        Pointer pReserved,
        IntByReference pdwNegotiatedVersion,
        PointerByReference phClientHandle
    );
    
    int WlanCloseHandle(
        Pointer hClientHandle,
        Pointer pReserved
    );
    
    int WlanEnumInterfaces(
        Pointer hClientHandle,
        Pointer pReserved,
        PointerByReference ppInterfaceList
    );
    
    int WlanScan(
        Pointer hClientHandle,
        Pointer pInterfaceGuid,
        Pointer pDot11Ssid,
        Pointer pIeData,
        Pointer pReserved
    );
    
    int WlanGetAvailableNetworkList(
        Pointer hClientHandle,
        Pointer pInterfaceGuid,
        int dwFlags,
        Pointer pReserved,
        PointerByReference ppAvailableNetworkList
    );
    
    void WlanFreeMemory(Pointer pMemory);
    
    // Estruturas
    class GUID extends Structure {
        public int Data1;
        public short Data2;
        public short Data3;
        public byte[] Data4 = new byte[8];
        
        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("Data1", "Data2", "Data3", "Data4");
        }
    }
    
    class WLAN_INTERFACE_INFO extends Structure {
        public GUID InterfaceGuid;
        public char[] strInterfaceDescription = new char[256];
        public int isState;
        
        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("InterfaceGuid", "strInterfaceDescription", "isState");
        }
    }
    
    class WLAN_INTERFACE_INFO_LIST extends Structure {
        public int dwNumberOfItems;
        public int dwIndex;
        public WLAN_INTERFACE_INFO[] InterfaceInfo = new WLAN_INTERFACE_INFO[1];
        
        public WLAN_INTERFACE_INFO_LIST() {}
        
        public WLAN_INTERFACE_INFO_LIST(Pointer p) {
            super(p);
            read();
        }
        
        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("dwNumberOfItems", "dwIndex", "InterfaceInfo");
        }
        
        @Override
        public void read() {
            super.read();
            InterfaceInfo = (WLAN_INTERFACE_INFO[]) InterfaceInfo[0].toArray(dwNumberOfItems);
        }
    }
    
    class WLAN_AVAILABLE_NETWORK extends Structure {
        public char[] strProfileName = new char[256];
        public DOT11_SSID dot11Ssid;
        public int dot11BssType;
        public int uNumberOfBssids;
        public boolean bNetworkConnectable;
        public int wlanNotConnectableReason;
        public int uNumberOfPhyTypes;
        public int[] dot11PhyTypes = new int[8];
        public boolean bMorePhyTypes;
        public int wlanSignalQuality;
        public boolean bSecurityEnabled;
        public int dot11DefaultAuthAlgorithm;
        public int dot11DefaultCipherAlgorithm;
        public int dwFlags;
        public int dwReserved;
        
        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("strProfileName", "dot11Ssid", "dot11BssType", "uNumberOfBssids",
                    "bNetworkConnectable", "wlanNotConnectableReason", "uNumberOfPhyTypes",
                    "dot11PhyTypes", "bMorePhyTypes", "wlanSignalQuality", "bSecurityEnabled",
                    "dot11DefaultAuthAlgorithm", "dot11DefaultCipherAlgorithm", "dwFlags", "dwReserved");
        }
    }
    
    class WLAN_AVAILABLE_NETWORK_LIST extends Structure {
        public int dwNumberOfItems;
        public int dwIndex;
        public WLAN_AVAILABLE_NETWORK[] Network = new WLAN_AVAILABLE_NETWORK[1];
        
        public WLAN_AVAILABLE_NETWORK_LIST() {}
        
        public WLAN_AVAILABLE_NETWORK_LIST(Pointer p) {
            super(p);
            read();
        }
        
        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("dwNumberOfItems", "dwIndex", "Network");
        }
        
        @Override
        public void read() {
            super.read();
            Network = (WLAN_AVAILABLE_NETWORK[]) Network[0].toArray(dwNumberOfItems);
        }
    }
    
    class DOT11_SSID extends Structure {
        public int uSSIDLength;
        public byte[] ucSSID = new byte[32];
        
        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("uSSIDLength", "ucSSID");
        }
        
        public String getSSID() {
            if (uSSIDLength == 0) return "";
            return new String(ucSSID, 0, uSSIDLength);
        }
    }
} 